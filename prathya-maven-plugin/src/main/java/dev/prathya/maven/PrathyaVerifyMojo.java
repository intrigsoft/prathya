package dev.prathya.maven;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.model.Severity;
import dev.prathya.core.report.HtmlReportWriter;
import dev.prathya.core.report.JsonReportWriter;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.TEST)
public class PrathyaVerifyMojo extends AbstractPrathyaMojo {

    @Parameter(property = "prathya.failOnViolations", defaultValue = "true")
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
                    outputDir.resolve("prathya-report.json"));
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
                            "Prathya verification failed with " + errorCount + " error violation(s)");
                }
            }

        } catch (PrathyaException e) {
            throw new MojoFailureException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
