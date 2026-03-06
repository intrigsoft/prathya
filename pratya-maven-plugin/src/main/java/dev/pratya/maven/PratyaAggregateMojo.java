package dev.pratya.maven;

import dev.pratya.core.model.AggregateReportData;
import dev.pratya.core.report.HtmlReportWriter;
import dev.pratya.core.report.JsonReportWriter;
import dev.pratya.core.report.ReportAggregator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "aggregate", aggregator = true, threadSafe = true,
      defaultPhase = LifecyclePhase.VERIFY)
public class PratyaAggregateMojo extends AbstractMojo {

    @Parameter(property = "pratya.aggregateOutputDirectory",
               defaultValue = "${project.build.directory}/pratya-aggregate")
    private String outputDirectory;

    @Parameter(property = "pratya.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("Pratya aggregate skipped.");
            return;
        }

        List<Path> reportPaths = new ArrayList<>();
        for (MavenProject project : reactorProjects) {
            if ("pom".equals(project.getPackaging())) {
                continue;
            }
            Path reportFile = Path.of(project.getBuild().getDirectory(), "pratya", "pratya-report.json");
            if (Files.exists(reportFile)) {
                reportPaths.add(reportFile);
                getLog().debug("Found report: " + reportFile);
            } else {
                getLog().warn("No pratya report found for module " + project.getArtifactId()
                        + " at " + reportFile);
            }
        }

        if (reportPaths.isEmpty()) {
            getLog().info("No pratya reports found in reactor modules. Nothing to aggregate.");
            return;
        }

        try {
            ReportAggregator aggregator = new ReportAggregator();
            AggregateReportData data = aggregator.aggregate(reportPaths);

            Path outputDir = Path.of(outputDirectory);
            Files.createDirectories(outputDir);

            new JsonReportWriter().writeAggregateJsonReport(data,
                    outputDir.resolve("pratya-aggregate-report.json"));
            new HtmlReportWriter().writeAggregateHtmlReport(data, outputDir);

            getLog().info("Pratya Aggregate Report:");
            getLog().info(String.format("  Modules: %d", data.getModules().size()));
            getLog().info(String.format("  Requirements: %d/%d covered (%.1f%%)",
                    data.getAggregateSummary().getCoveredRequirements(),
                    data.getAggregateSummary().getActiveRequirements(),
                    data.getAggregateSummary().getRequirementCoverage()));
            getLog().info(String.format("  Corner cases: %d/%d covered (%.1f%%)",
                    data.getAggregateSummary().getCoveredCornerCases(),
                    data.getAggregateSummary().getTotalCornerCases(),
                    data.getAggregateSummary().getCornerCaseCoverage()));
            getLog().info(String.format("  Violations: %d", data.getAllViolations().size()));
            getLog().info("  Reports: " + outputDir);

        } catch (IOException e) {
            throw new MojoFailureException("Failed to generate aggregate report: " + e.getMessage(), e);
        }
    }
}
