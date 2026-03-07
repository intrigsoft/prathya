package dev.prathya.maven;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.audit.AuditEngine;
import dev.prathya.core.audit.DefaultAuditEngine;
import dev.prathya.core.coverage.CoverageComputer;
import dev.prathya.core.coverage.DefaultCoverageComputer;
import dev.prathya.core.model.*;
import dev.prathya.core.parser.RequirementParser;
import dev.prathya.core.parser.YamlRequirementParser;
import dev.prathya.core.report.JacocoReportParser;
import dev.prathya.core.scanner.AnnotationScanner;
import dev.prathya.core.scanner.ReflectionAnnotationScanner;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPrathyaMojo extends AbstractMojo {

    @Parameter(property = "prathya.contractFile", defaultValue = "${project.basedir}/CONTRACT.yaml")
    protected String contractFile;

    @Parameter(property = "prathya.classesDirectory", defaultValue = "${project.build.outputDirectory}")
    protected String classesDirectory;

    @Parameter(property = "prathya.testClassesDirectory", defaultValue = "${project.build.testOutputDirectory}")
    protected String testClassesDirectory;

    @Parameter(property = "prathya.outputDirectory", defaultValue = "${project.build.directory}/prathya")
    protected String outputDirectory;

    @Parameter(property = "prathya.skip", defaultValue = "false")
    protected boolean skip;

    @Parameter
    protected List<String> excludeStatuses;

    @Parameter(property = "prathya.minimumRequirementCoverage", defaultValue = "0")
    protected double minimumRequirementCoverage;

    @Parameter(property = "prathya.minimumCornerCaseCoverage", defaultValue = "0")
    protected double minimumCornerCaseCoverage;

    @Parameter(property = "prathya.jacocoReportFile",
               defaultValue = "${project.build.directory}/site/jacoco/jacoco.xml")
    protected String jacocoReportFile;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    protected PipelineResult runPipeline() throws PrathyaException {
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

        // 3. Scan — include full test classpath so nested/inner classes can be loaded
        AnnotationScanner scanner = new ReflectionAnnotationScanner();
        Path testDir = Path.of(testClassesDirectory);
        List<Path> additionalClasspath = new ArrayList<>();
        additionalClasspath.add(Path.of(classesDirectory));
        if (project != null) {
            try {
                for (String element : project.getTestClasspathElements()) {
                    Path p = Path.of(element);
                    if (!p.equals(testDir) && Files.exists(p)) {
                        additionalClasspath.add(p);
                    }
                }
            } catch (Exception e) {
                getLog().debug("Could not resolve test classpath: " + e.getMessage());
            }
        }
        List<TraceEntry> traces = scanner.scan(List.of(testDir), additionalClasspath);

        // 4. Compute coverage
        CoverageComputer coverageComputer = new DefaultCoverageComputer();
        CoverageMatrix matrix = coverageComputer.compute(contract, traces);

        // 4b. Read JaCoCo code coverage if available
        CodeCoverageSummary codeCoverage = null;
        Path jacocoPath = Path.of(jacocoReportFile);
        if (Files.exists(jacocoPath)) {
            try {
                codeCoverage = new JacocoReportParser().parse(jacocoPath);
                getLog().info("  JaCoCo report found: " + jacocoPath);
            } catch (IOException e) {
                getLog().warn("  Failed to read JaCoCo report: " + e.getMessage());
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
            getLog().info("Prathya verification skipped.");
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

        int coveredItems = matrix.getSummary().getCoveredRequirements()
                + matrix.getSummary().getCoveredCornerCases();
        int totalItems = matrix.getSummary().getActiveRequirements()
                + matrix.getSummary().getTotalCornerCases();
        double contractCoverage = totalItems == 0 ? 0.0 : (double) coveredItems / totalItems * 100;

        getLog().info("Prathya Report: " + matrix.getModule().getId());
        getLog().info(String.format("  Contract coverage: %d/%d items (%.1f%%)",
                coveredItems, totalItems, contractCoverage));
        getLog().info(String.format("    Requirements: %d/%d  |  Corner cases: %d/%d",
                matrix.getSummary().getCoveredRequirements(),
                matrix.getSummary().getActiveRequirements(),
                matrix.getSummary().getCoveredCornerCases(),
                matrix.getSummary().getTotalCornerCases()));
        getLog().info(String.format("  Violations: %d (%d error, %d warn)",
                violations.size(), errorCount, warnCount));

        if (matrix.getCodeCoverage() != null) {
            CodeCoverageSummary cc = matrix.getCodeCoverage();
            getLog().info(String.format("  Code coverage: %.1f%% lines, %.1f%% branches",
                    cc.getLineRate(), cc.getBranchRate()));
        }
        if (matrix.getContractCodeCoverage() != null) {
            CodeCoverageSummary ccc = matrix.getContractCodeCoverage();
            getLog().info(String.format("  Contract code coverage: %.1f%% lines, %.1f%% branches",
                    ccc.getLineRate(), ccc.getBranchRate()));
        }
    }

    public record PipelineResult(CoverageMatrix matrix, List<Violation> violations) {
    }
}
