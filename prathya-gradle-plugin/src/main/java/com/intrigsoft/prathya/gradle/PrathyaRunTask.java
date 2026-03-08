package com.intrigsoft.prathya.gradle;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.audit.AuditEngine;
import com.intrigsoft.prathya.core.audit.DefaultAuditEngine;
import com.intrigsoft.prathya.core.coverage.DefaultCoverageComputer;
import com.intrigsoft.prathya.core.model.*;
import com.intrigsoft.prathya.core.parser.YamlRequirementParser;
import com.intrigsoft.prathya.core.report.HtmlReportWriter;
import com.intrigsoft.prathya.core.report.JsonReportWriter;
import com.intrigsoft.prathya.core.runner.DefaultPrathyaTestRunner;
import com.intrigsoft.prathya.core.runner.PrathyaTestRunner;
import com.intrigsoft.prathya.core.runner.TestClassifier;
import com.intrigsoft.prathya.core.runner.TestScope;
import com.intrigsoft.prathya.core.scanner.ReflectionAnnotationScanner;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PrathyaRunTask extends AbstractPrathyaTask {

    @Input
    @Optional
    public abstract Property<String> getRequirementId();

    @Input
    public abstract Property<String> getStatusFilter();

    @Input
    public abstract Property<Boolean> getFailOnTestFailure();

    @Input
    public abstract Property<String> getTestScopeName();

    @Input
    public abstract ListProperty<String> getIntegrationTestPatterns();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getTestReportsDir();

    @TaskAction
    public void run() {
        if (shouldSkip()) {
            return;
        }

        try {
            Path contractPath = getContractFile().getAsFile().get().toPath();
            Path testDir = getTestClassesDir().getAsFile().get().toPath();

            // 1. Parse CONTRACT.yaml
            ModuleContract contract = new YamlRequirementParser().parse(contractPath);

            // 2. Filter excluded statuses
            List<String> excluded = getExcludeStatuses().getOrElse(List.of());
            if (!excluded.isEmpty()) {
                Set<RequirementStatus> excludedSet = excluded.stream()
                        .map(s -> RequirementStatus.valueOf(s.toUpperCase()))
                        .collect(Collectors.toSet());
                List<RequirementDefinition> filtered = contract.getRequirements().stream()
                        .filter(r -> !excludedSet.contains(r.getStatus()))
                        .collect(Collectors.toList());
                contract = new ModuleContract(contract.getModule(), filtered);
            }

            // 3. Scan annotations
            Path classesDir = getClassesDir().getAsFile().get().toPath();
            List<TraceEntry> traces = new ReflectionAnnotationScanner().scan(List.of(testDir), List.of(classesDir));

            // 4. Resolve tests
            PrathyaTestRunner runner = new DefaultPrathyaTestRunner();
            String statusFilterValue = getStatusFilter().getOrElse("approved");
            RequirementStatus parsedStatus = RequirementStatus.valueOf(statusFilterValue.toUpperCase());
            String reqId = getRequirementId().getOrNull();
            List<TestMethod> tests = runner.resolveTests(contract, traces, parsedStatus, reqId);

            if (tests.isEmpty()) {
                getLogger().lifecycle("No tests resolved for the given filters. Skipping test execution.");
                return;
            }

            // 5. Classify and execute by scope
            TestScope scope = TestScope.fromString(getTestScopeName().getOrElse("unit"));
            List<String> itPatterns = getIntegrationTestPatterns().getOrElse(List.of());
            TestClassifier classifier = !itPatterns.isEmpty()
                    ? new TestClassifier(itPatterns)
                    : new TestClassifier();

            Path testReportsDir = getTestReportsDir().getAsFile().get().toPath();
            TestRunResult testRunResult;

            if (scope == TestScope.UNIT) {
                List<TestMethod> unitTests = classifier.filter(tests, TestScope.UNIT);
                if (unitTests.isEmpty()) {
                    getLogger().lifecycle("No unit tests to run.");
                    return;
                }
                List<String> filters = GradleTestFilterBuilder.build(unitTests);
                getLogger().lifecycle("Running {} unit test(s)", unitTests.size());
                executeGradleTests(filters);
                testRunResult = runner.parseResults(testReportsDir, unitTests);

            } else if (scope == TestScope.INTEGRATION) {
                List<TestMethod> itTests = classifier.filter(tests, TestScope.INTEGRATION);
                if (itTests.isEmpty()) {
                    getLogger().lifecycle("No integration tests to run.");
                    return;
                }
                List<String> filters = GradleTestFilterBuilder.build(itTests);
                getLogger().lifecycle("Running {} integration test(s)", itTests.size());
                executeGradleTests(filters);
                testRunResult = runner.parseResults(testReportsDir, itTests);

            } else {
                // ALL — partition and run both
                Map<TestScope, List<TestMethod>> partitioned = classifier.partition(tests);
                List<TestMethod> unitTests = partitioned.getOrDefault(TestScope.UNIT, Collections.emptyList());
                List<TestMethod> itTests = partitioned.getOrDefault(TestScope.INTEGRATION, Collections.emptyList());

                TestRunResult unitResult = null;
                TestRunResult itResult = null;

                if (!unitTests.isEmpty()) {
                    List<String> unitFilters = GradleTestFilterBuilder.build(unitTests);
                    getLogger().lifecycle("Running {} unit test(s)", unitTests.size());
                    executeGradleTests(unitFilters);
                    unitResult = runner.parseResults(testReportsDir, unitTests);
                }

                if (!itTests.isEmpty()) {
                    List<String> itFilters = GradleTestFilterBuilder.build(itTests);
                    getLogger().lifecycle("Running {} integration test(s)", itTests.size());
                    executeGradleTests(itFilters);
                    itResult = runner.parseResults(testReportsDir, itTests);
                }

                if (unitResult != null && itResult != null) {
                    testRunResult = TestRunResult.merge(unitResult, itResult);
                } else if (unitResult != null) {
                    testRunResult = unitResult;
                } else if (itResult != null) {
                    testRunResult = itResult;
                } else {
                    getLogger().lifecycle("No tests to run for scope ALL.");
                    return;
                }
            }

            getLogger().lifecycle("Test results: {} total, {} passed, {} failed, {} errors, {} skipped",
                    testRunResult.getTotalTests(), testRunResult.getPassed(),
                    testRunResult.getFailed(), testRunResult.getErrors(), testRunResult.getSkipped());

            // Compute three-state coverage
            DefaultCoverageComputer coverageComputer = new DefaultCoverageComputer();
            CoverageMatrix matrix = coverageComputer.compute(contract, traces, testRunResult);

            // Audit + threshold checks
            AuditEngine auditEngine = new DefaultAuditEngine();
            List<Violation> violations = new ArrayList<>(auditEngine.audit(contract, traces));

            double minReqCov = getMinRequirementCoverage().getOrElse(0.0);
            if (minReqCov > 0 && matrix.getSummary().getRequirementCoverage() < minReqCov) {
                violations.add(new Violation(
                        ViolationType.COVERAGE_BELOW_THRESHOLD,
                        null, null,
                        String.format("Requirement coverage %.1f%% is below minimum %.1f%%",
                                matrix.getSummary().getRequirementCoverage(), minReqCov)));
            }
            double minCcCov = getMinCornerCaseCoverage().getOrElse(0.0);
            if (minCcCov > 0 && matrix.getSummary().getCornerCaseCoverage() < minCcCov) {
                violations.add(new Violation(
                        ViolationType.COVERAGE_BELOW_THRESHOLD,
                        null, null,
                        String.format("Corner case coverage %.1f%% is below minimum %.1f%%",
                                matrix.getSummary().getCornerCaseCoverage(), minCcCov)));
            }

            // Write reports
            Path outputDir = getOutputDir().getAsFile().get().toPath();
            Files.createDirectories(outputDir);
            new JsonReportWriter().writeJsonReport(matrix, violations,
                    outputDir.resolve("prathya-report.json"));
            new HtmlReportWriter().writeHtmlReport(matrix, violations, outputDir);

            // Log summary
            PipelineResult result = new PipelineResult(matrix, violations);
            logSummary(result);
            getLogger().lifecycle("  Reports: {}", outputDir);

            // Fail if tests are failing
            if (getFailOnTestFailure().getOrElse(true) && !testRunResult.isAllPassing()) {
                throw new GradleException(String.format(
                        "Prathya test run failed: %d failed, %d errors out of %d tests",
                        testRunResult.getFailed(), testRunResult.getErrors(), testRunResult.getTotalTests()));
            }

        } catch (PrathyaException e) {
            throw new GradleException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new GradleException("Failed to write reports: " + e.getMessage(), e);
        }
    }

    private void executeGradleTests(List<String> testFilters) {
        getProject().javaexec(spec -> {
            // Use Gradle's test task execution via project.exec with gradle wrapper
            // This delegates to the project's test infrastructure
        });

        // Execute tests via the Gradle test task with --tests filters
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<String> args = new ArrayList<>();
        args.add("test");
        for (String filter : testFilters) {
            args.add("--tests");
            args.add(filter);
        }

        getProject().exec(execSpec -> {
            execSpec.setWorkingDir(getProject().getProjectDir());
            execSpec.setExecutable(resolveGradleExecutable());
            execSpec.args(args);
            execSpec.setStandardOutput(output);
            execSpec.setIgnoreExitValue(true);
        });
    }

    private String resolveGradleExecutable() {
        Path wrapper = getProject().getRootDir().toPath().resolve("gradlew");
        if (Files.exists(wrapper)) {
            return wrapper.toString();
        }
        return "gradle";
    }
}
