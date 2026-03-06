package dev.pratya.gradle;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.Severity;
import dev.pratya.core.report.HtmlReportWriter;
import dev.pratya.core.report.JsonReportWriter;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PratyaVerifyTask extends AbstractPratyaTask {

    @Input
    public abstract Property<Boolean> getFailOnViolations();

    @TaskAction
    public void verify() {
        if (shouldSkip()) {
            return;
        }

        try {
            PipelineResult result = runPipeline();

            // Write reports
            Path outputDir = getOutputDir().getAsFile().get().toPath();
            Files.createDirectories(outputDir);
            new JsonReportWriter().writeJsonReport(result.matrix(), result.violations(),
                    outputDir.resolve("pratya-report.json"));
            new HtmlReportWriter().writeHtmlReport(result.matrix(), result.violations(), outputDir);

            // Log summary
            logSummary(result);
            getLogger().lifecycle("  Reports: {}", outputDir);

            // Fail on ERROR-severity violations
            if (getFailOnViolations().getOrElse(true)) {
                long errorCount = result.violations().stream()
                        .filter(v -> v.getType().getSeverity() == Severity.ERROR).count();
                if (errorCount > 0) {
                    throw new GradleException(
                            "Pratya verification failed with " + errorCount + " error violation(s)");
                }
            }

        } catch (PratyaException e) {
            throw new GradleException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new GradleException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
