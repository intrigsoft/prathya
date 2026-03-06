package dev.pactum.maven;

import dev.pactum.core.PactumException;
import dev.pactum.core.report.HtmlReportWriter;
import dev.pactum.core.report.JsonReportWriter;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class PactumReportMojo extends AbstractPactumMojo {

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
                    outputDir.resolve("pactum-report.json"));
            new HtmlReportWriter().writeHtmlReport(result.matrix(), result.violations(), outputDir);

            // Log summary
            logSummary(result);
            getLog().info("  Reports: " + outputDir);

        } catch (PactumException e) {
            throw new MojoFailureException("Failed to parse REQUIREMENT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
