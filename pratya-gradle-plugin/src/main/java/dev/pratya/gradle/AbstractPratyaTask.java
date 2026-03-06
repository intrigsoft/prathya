package dev.pratya.gradle;

import dev.pratya.core.PratyaException;
import dev.pratya.core.audit.AuditEngine;
import dev.pratya.core.audit.DefaultAuditEngine;
import dev.pratya.core.coverage.CoverageComputer;
import dev.pratya.core.coverage.DefaultCoverageComputer;
import dev.pratya.core.model.*;
import dev.pratya.core.parser.RequirementParser;
import dev.pratya.core.parser.YamlRequirementParser;
import dev.pratya.core.scanner.AnnotationScanner;
import dev.pratya.core.scanner.ReflectionAnnotationScanner;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPratyaTask extends DefaultTask {

    public abstract RegularFileProperty getContractFile();

    public abstract DirectoryProperty getTestClassesDir();

    public abstract DirectoryProperty getOutputDir();

    public abstract ListProperty<String> getExcludeStatuses();

    public abstract Property<Double> getMinRequirementCoverage();

    public abstract Property<Double> getMinCornerCaseCoverage();

    protected PipelineResult runPipeline() throws PratyaException {
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
        List<TraceEntry> traces = scanner.scan(List.of(testDir));

        // 4. Compute coverage
        CoverageComputer coverageComputer = new DefaultCoverageComputer();
        CoverageMatrix matrix = coverageComputer.compute(contract, traces);

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

        getLogger().lifecycle("Pratya Report: {}", matrix.getModule().getId());
        getLogger().lifecycle("  Requirements: {}/{} covered ({:.1f}%)",
                matrix.getSummary().getCoveredRequirements(),
                matrix.getSummary().getActiveRequirements(),
                matrix.getSummary().getRequirementCoverage());
        getLogger().lifecycle("  Corner cases: {}/{} covered ({:.1f}%)",
                matrix.getSummary().getCoveredCornerCases(),
                matrix.getSummary().getTotalCornerCases(),
                matrix.getSummary().getCornerCaseCoverage());
        getLogger().lifecycle("  Violations: {} ({} error, {} warn)",
                violations.size(), errorCount, warnCount);
    }

    public record PipelineResult(CoverageMatrix matrix, List<Violation> violations) {
    }
}
