package dev.pactum.maven;

import dev.pactum.core.PactumException;
import dev.pactum.core.audit.AuditEngine;
import dev.pactum.core.audit.DefaultAuditEngine;
import dev.pactum.core.coverage.CoverageComputer;
import dev.pactum.core.coverage.DefaultCoverageComputer;
import dev.pactum.core.model.*;
import dev.pactum.core.parser.RequirementParser;
import dev.pactum.core.parser.YamlRequirementParser;
import dev.pactum.core.report.HtmlReportWriter;
import dev.pactum.core.report.JsonReportWriter;
import dev.pactum.core.scanner.AnnotationScanner;
import dev.pactum.core.scanner.ReflectionAnnotationScanner;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class PactumVerifyMojo extends AbstractMojo {

    @Parameter(property = "pactum.requirementFile", defaultValue = "${project.basedir}/REQUIREMENT.yaml")
    private String requirementFile;

    @Parameter(property = "pactum.testClassesDirectory", defaultValue = "${project.build.testOutputDirectory}")
    private String testClassesDirectory;

    @Parameter(property = "pactum.outputDirectory", defaultValue = "${project.build.directory}/pactum")
    private String outputDirectory;

    @Parameter(property = "pactum.failOnViolations", defaultValue = "true")
    private boolean failOnViolations;

    @Parameter(property = "pactum.minimumRequirementCoverage", defaultValue = "0")
    private double minimumRequirementCoverage;

    @Parameter(property = "pactum.skip", defaultValue = "false")
    private boolean skip;

    @Parameter
    private List<String> excludeStatuses;

    @Override
    public void execute() throws MojoFailureException {
        Path reqFile = Path.of(requirementFile);

        if (skip) {
            getLog().info("Pactum verification skipped.");
            return;
        }

        if (!Files.exists(reqFile)) {
            getLog().info("No REQUIREMENT.yaml found at " + reqFile + ", skipping.");
            return;
        }

        try {
            // 1. Parse
            RequirementParser parser = new YamlRequirementParser();
            ModuleContract contract = parser.parse(reqFile);

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

            // 6. Check threshold
            if (minimumRequirementCoverage > 0
                    && matrix.getSummary().getRequirementCoverage() < minimumRequirementCoverage) {
                violations.add(new Violation(
                        ViolationType.COVERAGE_BELOW_THRESHOLD,
                        null, null,
                        String.format("Requirement coverage %.1f%% is below minimum %.1f%%",
                                matrix.getSummary().getRequirementCoverage(), minimumRequirementCoverage)));
            }

            // 7. Write reports
            Path outputDir = Path.of(outputDirectory);
            Files.createDirectories(outputDir);

            new JsonReportWriter().writeJsonReport(matrix, violations, outputDir.resolve("pactum-report.json"));
            new HtmlReportWriter().writeHtmlReport(matrix, violations, outputDir);

            // 8. Log summary
            getLog().info("Pactum Report: " + contract.getModule().getId());
            getLog().info(String.format("  Requirements: %d/%d covered (%.1f%%)",
                    matrix.getSummary().getCoveredRequirements(),
                    matrix.getSummary().getActiveRequirements(),
                    matrix.getSummary().getRequirementCoverage()));
            getLog().info(String.format("  Corner cases: %d/%d covered (%.1f%%)",
                    matrix.getSummary().getCoveredCornerCases(),
                    matrix.getSummary().getTotalCornerCases(),
                    matrix.getSummary().getCornerCaseCoverage()));
            getLog().info("  Violations: " + violations.size());
            getLog().info("  Reports: " + outputDir);

            // 9. Fail if needed
            if (failOnViolations && !violations.isEmpty()) {
                throw new MojoFailureException("Pactum verification failed with " + violations.size() + " violation(s)");
            }

        } catch (PactumException e) {
            throw new MojoFailureException("Failed to parse REQUIREMENT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
