package dev.pratya.maven;

import dev.pratya.core.PratyaException;
import dev.pratya.core.audit.AuditEngine;
import dev.pratya.core.audit.DefaultAuditEngine;
import dev.pratya.core.coverage.DefaultCoverageComputer;
import dev.pratya.core.model.*;
import dev.pratya.core.parser.YamlRequirementParser;
import dev.pratya.core.report.HtmlReportWriter;
import dev.pratya.core.report.JsonReportWriter;
import dev.pratya.core.runner.DefaultPratyaTestRunner;
import dev.pratya.core.runner.PratyaTestRunner;
import dev.pratya.core.runner.SurefireFilterBuilder;
import dev.pratya.core.runner.TestClassifier;
import dev.pratya.core.runner.TestScope;
import dev.pratya.core.scanner.ReflectionAnnotationScanner;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "run", threadSafe = true)
public class PratyaRunMojo extends AbstractPratyaMojo {

    @Parameter(property = "pratya.requirementId")
    private String requirementId;

    @Parameter(property = "pratya.statusFilter", defaultValue = "approved")
    private String statusFilter;

    @Parameter(property = "pratya.failOnTestFailure", defaultValue = "true")
    private boolean failOnTestFailure;

    @Parameter(property = "pratya.testScope", defaultValue = "unit")
    private String testScope;

    @Parameter(defaultValue = "${project.build.directory}/surefire-reports", readonly = true)
    private String surefireReportsDirectory;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports", readonly = true)
    private String failsafeReportsDirectory;

    @Parameter(property = "pratya.integrationTestPatterns")
    private List<String> integrationTestPatterns;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File projectBasedir;

    @Parameter(defaultValue = "${maven.home}", readonly = true)
    private String mavenHome;

    @Override
    public void execute() throws MojoFailureException {
        if (shouldSkip()) {
            return;
        }

        try {
            // 1. Parse CONTRACT.yaml
            ModuleContract contract = new YamlRequirementParser().parse(Path.of(contractFile));

            // 2. Filter excluded statuses
            if (excludeStatuses != null && !excludeStatuses.isEmpty()) {
                Set<RequirementStatus> excluded = excludeStatuses.stream()
                        .map(s -> RequirementStatus.valueOf(s.toUpperCase()))
                        .collect(Collectors.toSet());
                List<RequirementDefinition> filtered = contract.getRequirements().stream()
                        .filter(r -> !excluded.contains(r.getStatus()))
                        .collect(Collectors.toList());
                contract = new ModuleContract(contract.getModule(), filtered);
            }

            // 3. Scan annotations
            Path testDir = Path.of(testClassesDirectory);
            Path classesDir = Path.of(classesDirectory);
            List<TraceEntry> traces = new ReflectionAnnotationScanner().scan(List.of(testDir), List.of(classesDir));

            // 4. Resolve tests
            PratyaTestRunner runner = new DefaultPratyaTestRunner();
            RequirementStatus parsedStatus = RequirementStatus.valueOf(statusFilter.toUpperCase());
            List<TestMethod> tests = runner.resolveTests(contract, traces, parsedStatus, requirementId);

            if (tests.isEmpty()) {
                getLog().info("No tests resolved for the given filters. Skipping test execution.");
                return;
            }

            // 5. Classify and execute by scope
            TestScope scope = TestScope.fromString(testScope);
            TestClassifier classifier = (integrationTestPatterns != null && !integrationTestPatterns.isEmpty())
                    ? new TestClassifier(integrationTestPatterns)
                    : new TestClassifier();

            TestRunResult testRunResult;

            if (scope == TestScope.UNIT) {
                List<TestMethod> unitTests = classifier.filter(tests, TestScope.UNIT);
                if (unitTests.isEmpty()) {
                    getLog().info("No unit tests to run.");
                    return;
                }
                String filter = SurefireFilterBuilder.build(unitTests);
                getLog().info("Running " + unitTests.size() + " unit test(s): " + filter);
                invokeMaven("test", "-Dtest=" + filter);
                testRunResult = runner.parseResults(Path.of(surefireReportsDirectory), unitTests);

            } else if (scope == TestScope.INTEGRATION) {
                List<TestMethod> itTests = classifier.filter(tests, TestScope.INTEGRATION);
                if (itTests.isEmpty()) {
                    getLog().info("No integration tests to run.");
                    return;
                }
                String filter = SurefireFilterBuilder.build(itTests);
                getLog().info("Running " + itTests.size() + " integration test(s): " + filter);
                invokeMaven("failsafe:integration-test", "-Dit.test=" + filter);
                testRunResult = runner.parseResults(Path.of(failsafeReportsDirectory), itTests);

            } else {
                // ALL — partition and run both
                Map<TestScope, List<TestMethod>> partitioned = classifier.partition(tests);
                List<TestMethod> unitTests = partitioned.getOrDefault(TestScope.UNIT, Collections.emptyList());
                List<TestMethod> itTests = partitioned.getOrDefault(TestScope.INTEGRATION, Collections.emptyList());

                TestRunResult unitResult = null;
                TestRunResult itResult = null;

                if (!unitTests.isEmpty()) {
                    String unitFilter = SurefireFilterBuilder.build(unitTests);
                    getLog().info("Running " + unitTests.size() + " unit test(s): " + unitFilter);
                    invokeMaven("test", "-Dtest=" + unitFilter);
                    unitResult = runner.parseResults(Path.of(surefireReportsDirectory), unitTests);
                }

                if (!itTests.isEmpty()) {
                    String itFilter = SurefireFilterBuilder.build(itTests);
                    getLog().info("Running " + itTests.size() + " integration test(s): " + itFilter);
                    invokeMaven("failsafe:integration-test", "-Dit.test=" + itFilter);
                    itResult = runner.parseResults(Path.of(failsafeReportsDirectory), itTests);
                }

                if (unitResult != null && itResult != null) {
                    testRunResult = TestRunResult.merge(unitResult, itResult);
                } else if (unitResult != null) {
                    testRunResult = unitResult;
                } else if (itResult != null) {
                    testRunResult = itResult;
                } else {
                    getLog().info("No tests to run for scope ALL.");
                    return;
                }
            }

            getLog().info(String.format("Test results: %d total, %d passed, %d failed, %d errors, %d skipped",
                    testRunResult.getTotalTests(), testRunResult.getPassed(),
                    testRunResult.getFailed(), testRunResult.getErrors(), testRunResult.getSkipped()));

            // 8. Compute three-state coverage
            DefaultCoverageComputer coverageComputer = new DefaultCoverageComputer();
            CoverageMatrix matrix = coverageComputer.compute(contract, traces, testRunResult);

            // 9. Audit + threshold checks
            AuditEngine auditEngine = new DefaultAuditEngine();
            List<Violation> violations = new ArrayList<>(auditEngine.audit(contract, traces));

            if (minimumRequirementCoverage > 0
                    && matrix.getSummary().getRequirementCoverage() < minimumRequirementCoverage) {
                violations.add(new Violation(
                        ViolationType.COVERAGE_BELOW_THRESHOLD,
                        null, null,
                        String.format("Requirement coverage %.1f%% is below minimum %.1f%%",
                                matrix.getSummary().getRequirementCoverage(), minimumRequirementCoverage)));
            }
            if (minimumCornerCaseCoverage > 0
                    && matrix.getSummary().getCornerCaseCoverage() < minimumCornerCaseCoverage) {
                violations.add(new Violation(
                        ViolationType.COVERAGE_BELOW_THRESHOLD,
                        null, null,
                        String.format("Corner case coverage %.1f%% is below minimum %.1f%%",
                                matrix.getSummary().getCornerCaseCoverage(), minimumCornerCaseCoverage)));
            }

            // 10. Write reports
            Path outputDir = Path.of(outputDirectory);
            Files.createDirectories(outputDir);
            new JsonReportWriter().writeJsonReport(matrix, violations,
                    outputDir.resolve("pratya-report.json"));
            new HtmlReportWriter().writeHtmlReport(matrix, violations, outputDir);

            // 11. Log summary
            PipelineResult result = new PipelineResult(matrix, violations);
            logSummary(result);
            getLog().info("  Reports: " + outputDir);

            // 12. Fail if tests are failing
            if (failOnTestFailure && !testRunResult.isAllPassing()) {
                throw new MojoFailureException(String.format(
                        "Pratya test run failed: %d failed, %d errors out of %d tests",
                        testRunResult.getFailed(), testRunResult.getErrors(), testRunResult.getTotalTests()));
            }

        } catch (PratyaException e) {
            throw new MojoFailureException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        } catch (MavenInvocationException e) {
            throw new MojoFailureException("Failed to invoke Maven: " + e.getMessage(), e);
        }
    }

    private void invokeMaven(String goal, String filterArg) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(projectBasedir, "pom.xml"));
        request.setGoals(Collections.singletonList(goal));
        request.addArg(filterArg);
        request.addArg("-Dsurefire.testFailureIgnore=true");
        request.addArg("-Dfailsafe.testFailureIgnore=true");
        request.setBatchMode(true);

        Invoker invoker = new DefaultInvoker();
        if (mavenHome != null) {
            invoker.setMavenHome(new File(mavenHome));
        }

        InvocationResult result = invoker.execute(request);
        // Do NOT throw on non-zero exit — tests may fail, which is expected.
        // The test results will be parsed from XML reports.
        if (result.getExitCode() != 0) {
            getLog().warn("Maven " + goal + " execution exited with code " + result.getExitCode());
        }
    }
}
