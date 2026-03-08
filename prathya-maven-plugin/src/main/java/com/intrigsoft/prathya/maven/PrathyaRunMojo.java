package com.intrigsoft.prathya.maven;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.audit.AuditEngine;
import com.intrigsoft.prathya.core.audit.DefaultAuditEngine;
import com.intrigsoft.prathya.core.coverage.DefaultCoverageComputer;
import com.intrigsoft.prathya.core.model.*;
import com.intrigsoft.prathya.core.parser.YamlRequirementParser;
import com.intrigsoft.prathya.core.report.HtmlReportWriter;
import com.intrigsoft.prathya.core.report.JacocoReportParser;
import com.intrigsoft.prathya.core.report.JsonReportWriter;
import com.intrigsoft.prathya.core.runner.DefaultPrathyaTestRunner;
import com.intrigsoft.prathya.core.runner.PrathyaTestRunner;
import com.intrigsoft.prathya.core.runner.SurefireFilterBuilder;
import com.intrigsoft.prathya.core.runner.TestClassifier;
import com.intrigsoft.prathya.core.runner.TestScope;
import com.intrigsoft.prathya.core.scanner.ReflectionAnnotationScanner;

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

@Mojo(name = "run", threadSafe = true)
public class PrathyaRunMojo extends AbstractPrathyaMojo {

    @Parameter(property = "prathya.requirementId")
    private String requirementId;

    @Parameter(property = "prathya.statusFilter", defaultValue = "approved")
    private String statusFilter;

    @Parameter(property = "prathya.failOnTestFailure", defaultValue = "true")
    private boolean failOnTestFailure;

    @Parameter(property = "prathya.testScope", defaultValue = "unit")
    private String testScope;

    @Parameter(defaultValue = "${project.build.directory}/surefire-reports", readonly = true)
    private String surefireReportsDirectory;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports", readonly = true)
    private String failsafeReportsDirectory;

    @Parameter(property = "prathya.integrationTestPatterns")
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
            ModuleContract fullContract = new YamlRequirementParser().parse(Path.of(contractFile));

            // 2. Filter inactive and user-excluded statuses for coverage/test resolution
            ModuleContract filteredContract = filterContract(fullContract);

            // 3. Scan annotations
            Path testDir = Path.of(testClassesDirectory);
            Path classesDir = Path.of(classesDirectory);
            List<TraceEntry> traces = new ReflectionAnnotationScanner().scan(List.of(testDir), List.of(classesDir));

            // 4. Resolve tests (uses filtered contract)
            PrathyaTestRunner runner = new DefaultPrathyaTestRunner();
            RequirementStatus parsedStatus = RequirementStatus.valueOf(statusFilter.toUpperCase());
            List<TestMethod> tests = runner.resolveTests(filteredContract, traces, parsedStatus, requirementId);

            if (tests.isEmpty()) {
                getLog().info("No tests resolved for the given filters. Skipping test execution.");
                return;
            }

            // 4b. Read existing JaCoCo report as total code coverage (from previous full test run)
            CodeCoverageSummary totalCodeCoverage = null;
            Path jacocoPath = Path.of(jacocoReportFile);
            if (Files.exists(jacocoPath)) {
                try {
                    totalCodeCoverage = new JacocoReportParser().parse(jacocoPath);
                    getLog().info("  JaCoCo report found (total code coverage): " + jacocoPath);
                } catch (IOException e) {
                    getLog().warn("  Failed to read JaCoCo report: " + e.getMessage());
                }
            }

            // 5. Classify and execute by scope
            // Contract-test JaCoCo exec file — separate from the project's main jacoco.exec
            Path buildDir = Path.of(outputDirectory).getParent(); // target/
            String contractExecFile = buildDir.resolve("prathya/jacoco-contract.exec").toString();
            String jacocoDestfileArg = "-Djacoco.destfile=" + contractExecFile;

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
                invokeMaven("test", "-Dtest=" + filter, jacocoDestfileArg);
                testRunResult = runner.parseResults(Path.of(surefireReportsDirectory), unitTests);

            } else if (scope == TestScope.INTEGRATION) {
                List<TestMethod> itTests = classifier.filter(tests, TestScope.INTEGRATION);
                if (itTests.isEmpty()) {
                    getLog().info("No integration tests to run.");
                    return;
                }
                String filter = SurefireFilterBuilder.build(itTests);
                getLog().info("Running " + itTests.size() + " integration test(s): " + filter);
                invokeMaven("failsafe:integration-test", "-Dit.test=" + filter, jacocoDestfileArg);
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
                    invokeMaven("test", "-Dtest=" + unitFilter, jacocoDestfileArg);
                    unitResult = runner.parseResults(Path.of(surefireReportsDirectory), unitTests);
                }

                if (!itTests.isEmpty()) {
                    String itFilter = SurefireFilterBuilder.build(itTests);
                    getLog().info("Running " + itTests.size() + " integration test(s): " + itFilter);
                    invokeMaven("failsafe:integration-test", "-Dit.test=" + itFilter, jacocoDestfileArg);
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

            // 8. Generate contract code coverage report (best-effort)
            CodeCoverageSummary contractCodeCoverage = null;
            Path contractExecPath = Path.of(contractExecFile);
            if (Files.exists(contractExecPath)) {
                try {
                    Path contractJacocoDir = buildDir.resolve("prathya/jacoco");
                    invokeJacocoReport(contractExecFile, contractJacocoDir.toString());
                    Path contractJacocoXml = contractJacocoDir.resolve("jacoco.xml");
                    if (Files.exists(contractJacocoXml)) {
                        contractCodeCoverage = new JacocoReportParser().parse(contractJacocoXml);
                        getLog().info("  Contract code coverage report generated: " + contractJacocoXml);
                    }
                } catch (Exception e) {
                    getLog().debug("  Could not generate contract code coverage report: " + e.getMessage());
                }
            }

            // 9. Compute three-state coverage (uses filtered contract)
            DefaultCoverageComputer coverageComputer = new DefaultCoverageComputer();
            CoverageMatrix matrix = coverageComputer.compute(filteredContract, traces, testRunResult);

            // Set code coverage data on the matrix
            if (totalCodeCoverage != null || contractCodeCoverage != null) {
                matrix = new CoverageMatrix(
                        matrix.getModule(), matrix.getSummary(),
                        matrix.getRequirements(), matrix.getViolations(),
                        matrix.getContract(), totalCodeCoverage, contractCodeCoverage);
            }

            // 11. Audit + threshold checks (uses full contract for deprecated/superseded detection)
            AuditEngine auditEngine = new DefaultAuditEngine();
            List<Violation> violations = new ArrayList<>(auditEngine.audit(fullContract, traces));

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

            // 12. Write reports
            Path outputDir = Path.of(outputDirectory);
            Files.createDirectories(outputDir);
            new JsonReportWriter().writeJsonReport(matrix, violations,
                    outputDir.resolve("prathya-report.json"));
            new HtmlReportWriter().writeHtmlReport(matrix, violations, outputDir);

            // 13. Log summary
            PipelineResult result = new PipelineResult(matrix, violations);
            logSummary(result);
            getLog().info("  Reports: " + outputDir);

            // 14. Fail if tests are failing
            if (failOnTestFailure && !testRunResult.isAllPassing()) {
                throw new MojoFailureException(String.format(
                        "Prathya test run failed: %d failed, %d errors out of %d tests",
                        testRunResult.getFailed(), testRunResult.getErrors(), testRunResult.getTotalTests()));
            }

        } catch (PrathyaException e) {
            throw new MojoFailureException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        } catch (MavenInvocationException e) {
            throw new MojoFailureException("Failed to invoke Maven: " + e.getMessage(), e);
        }
    }

    private void invokeMaven(String goal, String... extraArgs) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(projectBasedir, "pom.xml"));
        request.setGoals(Collections.singletonList(goal));
        for (String arg : extraArgs) {
            request.addArg(arg);
        }
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

    private void invokeJacocoReport(String dataFile, String outputDirectory) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(projectBasedir, "pom.xml"));
        request.setGoals(Collections.singletonList("jacoco:report"));
        request.addArg("-Djacoco.dataFile=" + dataFile);
        request.addArg("-Djacoco.outputDirectory=" + outputDirectory);
        request.setBatchMode(true);

        Invoker invoker = new DefaultInvoker();
        if (mavenHome != null) {
            invoker.setMavenHome(new File(mavenHome));
        }

        InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            getLog().debug("jacoco:report exited with code " + result.getExitCode()
                    + " (JaCoCo may not be configured)");
        }
    }
}
