package dev.pactum.maven;

import dev.pactum.core.PactumException;
import dev.pactum.core.audit.AuditEngine;
import dev.pactum.core.audit.DefaultAuditEngine;
import dev.pactum.core.coverage.DefaultCoverageComputer;
import dev.pactum.core.model.*;
import dev.pactum.core.parser.YamlRequirementParser;
import dev.pactum.core.report.HtmlReportWriter;
import dev.pactum.core.report.JsonReportWriter;
import dev.pactum.core.runner.DefaultPactumTestRunner;
import dev.pactum.core.runner.PactumTestRunner;
import dev.pactum.core.runner.SurefireFilterBuilder;
import dev.pactum.core.scanner.ReflectionAnnotationScanner;

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
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "run", threadSafe = true)
public class PactumRunMojo extends AbstractPactumMojo {

    @Parameter(property = "pactum.requirementId")
    private String requirementId;

    @Parameter(property = "pactum.statusFilter", defaultValue = "approved")
    private String statusFilter;

    @Parameter(property = "pactum.failOnTestFailure", defaultValue = "true")
    private boolean failOnTestFailure;

    @Parameter(defaultValue = "${project.build.directory}/surefire-reports", readonly = true)
    private String surefireReportsDirectory;

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
            // 1. Parse REQUIREMENT.yaml
            ModuleContract contract = new YamlRequirementParser().parse(Path.of(requirementFile));

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
            List<TraceEntry> traces = new ReflectionAnnotationScanner().scan(List.of(testDir));

            // 4. Resolve tests
            PactumTestRunner runner = new DefaultPactumTestRunner();
            RequirementStatus parsedStatus = RequirementStatus.valueOf(statusFilter.toUpperCase());
            List<TestMethod> tests = runner.resolveTests(contract, traces, parsedStatus, requirementId);

            if (tests.isEmpty()) {
                getLog().info("No tests resolved for the given filters. Skipping test execution.");
                return;
            }

            // 5. Build Surefire filter
            String filter = SurefireFilterBuilder.build(tests);
            getLog().info("Running " + tests.size() + " test(s): " + filter);

            // 6. Invoke mvn test
            invokeMavenTest(filter);

            // 7. Parse results
            Path reportsDir = Path.of(surefireReportsDirectory);
            TestRunResult testRunResult = runner.parseResults(reportsDir, tests);

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
                    outputDir.resolve("pactum-report.json"));
            new HtmlReportWriter().writeHtmlReport(matrix, violations, outputDir);

            // 11. Log summary
            PipelineResult result = new PipelineResult(matrix, violations);
            logSummary(result);
            getLog().info("  Reports: " + outputDir);

            // 12. Fail if tests are failing
            if (failOnTestFailure && !testRunResult.isAllPassing()) {
                throw new MojoFailureException(String.format(
                        "Pactum test run failed: %d failed, %d errors out of %d tests",
                        testRunResult.getFailed(), testRunResult.getErrors(), testRunResult.getTotalTests()));
            }

        } catch (PactumException e) {
            throw new MojoFailureException("Failed to parse REQUIREMENT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        } catch (MavenInvocationException e) {
            throw new MojoFailureException("Failed to invoke Maven: " + e.getMessage(), e);
        }
    }

    private void invokeMavenTest(String testFilter) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(projectBasedir, "pom.xml"));
        request.setGoals(Collections.singletonList("test"));
        request.addArg("-Dtest=" + testFilter);
        request.setBatchMode(true);

        Invoker invoker = new DefaultInvoker();
        if (mavenHome != null) {
            invoker.setMavenHome(new File(mavenHome));
        }

        InvocationResult result = invoker.execute(request);
        // Do NOT throw on non-zero exit — tests may fail, which is expected.
        // The test results will be parsed from XML reports.
        if (result.getExitCode() != 0) {
            getLog().warn("Maven test execution exited with code " + result.getExitCode());
        }
    }
}
