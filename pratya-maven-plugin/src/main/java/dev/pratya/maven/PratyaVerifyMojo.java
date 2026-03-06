package dev.pratya.maven;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.Severity;
import dev.pratya.core.report.HtmlReportWriter;
import dev.pratya.core.report.JsonReportWriter;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class PratyaVerifyMojo extends AbstractPratyaMojo {

    @Parameter(property = "pratya.failOnViolations", defaultValue = "true")
    private boolean failOnViolations;

    @Override
    public void execute() throws MojoFailureException {
        if (shouldSkip()) {
            return;
        }

        try {
            PipelineResult result = runPipeline();

            // Write reports
            Path outputDir = Path.of(outputDirectory);
            Files.createDirectories(outputDir);
            new JsonReportWriter().writeJsonReport(result.matrix(), result.violations(),
                    outputDir.resolve("pratya-report.json"));
            new HtmlReportWriter().writeHtmlReport(result.matrix(), result.violations(), outputDir);

            // Log summary
            logSummary(result);
            getLog().info("  Reports: " + outputDir);

            // Fail only on ERROR-severity violations
            if (failOnViolations) {
                long errorCount = result.violations().stream()
                        .filter(v -> v.getType().getSeverity() == Severity.ERROR).count();
                if (errorCount > 0) {
                    throw new MojoFailureException(
                            "Pratya verification failed with " + errorCount + " error violation(s)");
                }
            }

        } catch (PratyaException e) {
            throw new MojoFailureException("Failed to parse REQUIREMENT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
