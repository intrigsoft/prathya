package dev.prathya.gradle;

import dev.prathya.core.model.AggregateReportData;
import dev.prathya.core.report.HtmlReportWriter;
import dev.prathya.core.report.JsonReportWriter;
import dev.prathya.core.report.ReportAggregator;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PrathyaAggregateTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getReportFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void aggregate() {
        List<Path> reportPaths = getReportFiles().getFiles().stream()
                .filter(f -> f.exists())
                .map(f -> f.toPath())
                .collect(Collectors.toList());

        if (reportPaths.isEmpty()) {
            getLogger().lifecycle("No prathya reports found. Nothing to aggregate.");
            return;
        }

        try {
            ReportAggregator aggregator = new ReportAggregator();
            AggregateReportData data = aggregator.aggregate(reportPaths);

            Path outputDir = getOutputDir().getAsFile().get().toPath();
            Files.createDirectories(outputDir);

            new JsonReportWriter().writeAggregateJsonReport(data,
                    outputDir.resolve("prathya-aggregate-report.json"));
            new HtmlReportWriter().writeAggregateHtmlReport(data, outputDir);

            getLogger().lifecycle("Prathya Aggregate Report:");
            getLogger().lifecycle("  Modules: {}", data.getModules().size());
            getLogger().lifecycle("  Requirements: {}/{} covered ({:.1f}%)",
                    data.getAggregateSummary().getCoveredRequirements(),
                    data.getAggregateSummary().getActiveRequirements(),
                    data.getAggregateSummary().getRequirementCoverage());
            getLogger().lifecycle("  Corner cases: {}/{} covered ({:.1f}%)",
                    data.getAggregateSummary().getCoveredCornerCases(),
                    data.getAggregateSummary().getTotalCornerCases(),
                    data.getAggregateSummary().getCornerCaseCoverage());
            getLogger().lifecycle("  Violations: {}", data.getAllViolations().size());
            getLogger().lifecycle("  Reports: {}", outputDir);

        } catch (IOException e) {
            throw new GradleException("Failed to generate aggregate report: " + e.getMessage(), e);
        }
    }
}
