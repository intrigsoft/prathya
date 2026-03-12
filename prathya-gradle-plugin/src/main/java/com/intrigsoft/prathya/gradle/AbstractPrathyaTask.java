package com.intrigsoft.prathya.gradle;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.audit.AuditEngine;
import com.intrigsoft.prathya.core.audit.DefaultAuditEngine;
import com.intrigsoft.prathya.core.coverage.CoverageComputer;
import com.intrigsoft.prathya.core.coverage.DefaultCoverageComputer;
import com.intrigsoft.prathya.core.model.*;
import com.intrigsoft.prathya.core.parser.RequirementParser;
import com.intrigsoft.prathya.core.parser.YamlRequirementParser;
import com.intrigsoft.prathya.core.report.JacocoReportParser;
import com.intrigsoft.prathya.core.scanner.AnnotationScanner;
import com.intrigsoft.prathya.core.scanner.NonContractualScanner;
import com.intrigsoft.prathya.core.scanner.ReflectionAnnotationScanner;
import com.intrigsoft.prathya.core.scanner.ReflectionNonContractualScanner;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPrathyaTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getContractFile();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getClassesDir();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getTestClassesDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    @Optional
    public abstract ListProperty<String> getExcludeStatuses();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public abstract RegularFileProperty getJacocoReportFile();

    @Input
    @Optional
    public abstract Property<Double> getMinRequirementCoverage();

    @Input
    @Optional
    public abstract Property<Double> getMinCornerCaseCoverage();

    protected PipelineResult runPipeline() throws PrathyaException {
        Path contractPath = getContractFile().getAsFile().get().toPath();
        Path testDir = getTestClassesDir().getAsFile().get().toPath();

        // 1. Parse
        RequirementParser parser = new YamlRequirementParser();
        ModuleContract contract = parser.parse(contractPath);

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

        // 3. Scan
        AnnotationScanner scanner = new ReflectionAnnotationScanner();
        Path classesDir = getClassesDir().getAsFile().get().toPath();
        List<TraceEntry> traces = scanner.scan(List.of(testDir), List.of(classesDir));

        // 3b. Scan production classes for @NonContractual exclusions
        NonContractualScanner ncScanner = new ReflectionNonContractualScanner();
        List<NonContractualEntry> exclusions = ncScanner.scan(List.of(classesDir));

        // 4. Compute coverage
        CoverageComputer coverageComputer = new DefaultCoverageComputer();
        CoverageMatrix matrix = coverageComputer.compute(contract, traces);

        // 4b. Read JaCoCo code coverage if available
        CodeCoverageSummary codeCoverage = null;
        if (getJacocoReportFile().isPresent()) {
            Path jacocoPath = getJacocoReportFile().getAsFile().get().toPath();
            if (Files.exists(jacocoPath)) {
                try {
                    codeCoverage = new JacocoReportParser().parse(jacocoPath, exclusions);
                    getLogger().lifecycle("  JaCoCo report found: {}", jacocoPath);
                    if (!exclusions.isEmpty()) {
                        getLogger().lifecycle("  Non-contractual exclusions: {}", exclusions.size());
                    }
                } catch (IOException e) {
                    getLogger().warn("  Failed to read JaCoCo report: {}", e.getMessage());
                }
            }
        }
        if (codeCoverage != null) {
            matrix = new CoverageMatrix(
                    matrix.getModule(), matrix.getSummary(),
                    matrix.getRequirements(), matrix.getViolations(),
                    matrix.getContract(), codeCoverage);
        }

        // 5. Audit
        AuditEngine auditEngine = new DefaultAuditEngine();
        List<Violation> violations = new ArrayList<>(auditEngine.audit(contract, traces));

        // 6. Check requirement coverage threshold
        double minReqCov = getMinRequirementCoverage().getOrElse(0.0);
        if (minReqCov > 0 && matrix.getSummary().getRequirementCoverage() < minReqCov) {
            violations.add(new Violation(
                    ViolationType.COVERAGE_BELOW_THRESHOLD,
                    null, null,
                    String.format("Requirement coverage %.1f%% is below minimum %.1f%%",
                            matrix.getSummary().getRequirementCoverage(), minReqCov)));
        }

        // 7. Check corner case coverage threshold
        double minCcCov = getMinCornerCaseCoverage().getOrElse(0.0);
        if (minCcCov > 0 && matrix.getSummary().getCornerCaseCoverage() < minCcCov) {
            violations.add(new Violation(
                    ViolationType.COVERAGE_BELOW_THRESHOLD,
                    null, null,
                    String.format("Corner case coverage %.1f%% is below minimum %.1f%%",
                            matrix.getSummary().getCornerCaseCoverage(), minCcCov)));
        }

        return new PipelineResult(matrix, violations);
    }

    protected boolean shouldSkip() {
        Path contractPath = getContractFile().getAsFile().get().toPath();
        if (!Files.exists(contractPath)) {
            getLogger().info("No CONTRACT.yaml found at {}, skipping.", contractPath);
            return true;
        }
        return false;
    }

    protected void logSummary(PipelineResult result) {
        CoverageMatrix matrix = result.matrix();
        List<Violation> violations = result.violations();

        long errorCount = violations.stream()
                .filter(v -> v.getType().getSeverity() == Severity.ERROR).count();
        long warnCount = violations.stream()
                .filter(v -> v.getType().getSeverity() == Severity.WARN).count();

        getLogger().lifecycle("Prathya Report: {}", matrix.getModule().getId());
        getLogger().lifecycle("  Requirements: {}/{} covered ({}%)",
                matrix.getSummary().getCoveredRequirements(),
                matrix.getSummary().getActiveRequirements(),
                String.format("%.1f", matrix.getSummary().getRequirementCoverage()));
        getLogger().lifecycle("  Corner cases: {}/{} covered ({}%)",
                matrix.getSummary().getCoveredCornerCases(),
                matrix.getSummary().getTotalCornerCases(),
                String.format("%.1f", matrix.getSummary().getCornerCaseCoverage()));
        getLogger().lifecycle("  Violations: {} ({} error, {} warn)",
                violations.size(), errorCount, warnCount);
    }

    public record PipelineResult(CoverageMatrix matrix, List<Violation> violations) {
    }
}
