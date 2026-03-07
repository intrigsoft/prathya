package dev.prathya.gradle;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.report.HtmlReportWriter;
import dev.prathya.core.report.JsonReportWriter;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PrathyaReportTask extends AbstractPrathyaTask {

    @TaskAction
    public void report() {
        if (shouldSkip()) {
            return;
        }

        try {
            PipelineResult result = runPipeline();

            // Write reports
            Path outputDir = getOutputDir().getAsFile().get().toPath();
            Files.createDirectories(outputDir);
            new JsonReportWriter().writeJsonReport(result.matrix(), result.violations(),
                    outputDir.resolve("prathya-report.json"));
            new HtmlReportWriter().writeHtmlReport(result.matrix(), result.violations(), outputDir);

            // Log summary
            logSummary(result);
            getLogger().lifecycle("  Reports: {}", outputDir);

        } catch (PrathyaException e) {
            throw new GradleException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new GradleException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
