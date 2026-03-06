package dev.pratya.maven;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPratyaMojo extends AbstractMojo {

    @Parameter(property = "pratya.contractFile", defaultValue = "${project.basedir}/CONTRACT.yaml")
    protected String contractFile;

    @Parameter(property = "pratya.testClassesDirectory", defaultValue = "${project.build.testOutputDirectory}")
    protected String testClassesDirectory;

    @Parameter(property = "pratya.outputDirectory", defaultValue = "${project.build.directory}/pratya")
    protected String outputDirectory;

    @Parameter(property = "pratya.skip", defaultValue = "false")
    protected boolean skip;

    @Parameter
    protected List<String> excludeStatuses;

    @Parameter(property = "pratya.minimumRequirementCoverage", defaultValue = "0")
    protected double minimumRequirementCoverage;

    @Parameter(property = "pratya.minimumCornerCaseCoverage", defaultValue = "0")
    protected double minimumCornerCaseCoverage;

    protected PipelineResult runPipeline() throws PratyaException {
        // 1. Parse
        RequirementParser parser = new YamlRequirementParser();
        ModuleContract contract = parser.parse(Path.of(contractFile));

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

        // 3. Scan
        AnnotationScanner scanner = new ReflectionAnnotationScanner();
        Path testDir = Path.of(testClassesDirectory);
        List<TraceEntry> traces = scanner.scan(List.of(testDir));

        // 4. Compute coverage
        CoverageComputer coverageComputer = new DefaultCoverageComputer();
        CoverageMatrix matrix = coverageComputer.compute(contract, traces);

        // 5. Audit
        AuditEngine auditEngine = new DefaultAuditEngine();
        List<Violation> violations = new ArrayList<>(auditEngine.audit(contract, traces));

        // 6. Check requirement coverage threshold
        if (minimumRequirementCoverage > 0
                && matrix.getSummary().getRequirementCoverage() < minimumRequirementCoverage) {
            violations.add(new Violation(
                    ViolationType.COVERAGE_BELOW_THRESHOLD,
                    null, null,
                    String.format("Requirement coverage %.1f%% is below minimum %.1f%%",
                            matrix.getSummary().getRequirementCoverage(), minimumRequirementCoverage)));
        }

        // 7. Check corner case coverage threshold
        if (minimumCornerCaseCoverage > 0
                && matrix.getSummary().getCornerCaseCoverage() < minimumCornerCaseCoverage) {
            violations.add(new Violation(
                    ViolationType.COVERAGE_BELOW_THRESHOLD,
                    null, null,
                    String.format("Corner case coverage %.1f%% is below minimum %.1f%%",
                            matrix.getSummary().getCornerCaseCoverage(), minimumCornerCaseCoverage)));
        }

        return new PipelineResult(matrix, violations);
    }

    protected boolean shouldSkip() {
        Path reqFile = Path.of(contractFile);

        if (skip) {
            getLog().info("Pratya verification skipped.");
            return true;
        }

        if (!Files.exists(reqFile)) {
            getLog().info("No CONTRACT.yaml found at " + reqFile + ", skipping.");
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

        getLog().info("Pratya Report: " + matrix.getModule().getId());
        getLog().info(String.format("  Requirements: %d/%d covered (%.1f%%)",
                matrix.getSummary().getCoveredRequirements(),
                matrix.getSummary().getActiveRequirements(),
                matrix.getSummary().getRequirementCoverage()));
        getLog().info(String.format("  Corner cases: %d/%d covered (%.1f%%)",
                matrix.getSummary().getCoveredCornerCases(),
                matrix.getSummary().getTotalCornerCases(),
                matrix.getSummary().getCornerCaseCoverage()));
        getLog().info(String.format("  Violations: %d (%d error, %d warn)",
                violations.size(), errorCount, warnCount));
    }

    public record PipelineResult(CoverageMatrix matrix, List<Violation> violations) {
    }
}
