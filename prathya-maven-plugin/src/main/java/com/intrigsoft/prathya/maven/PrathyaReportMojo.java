package com.intrigsoft.prathya.maven;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.report.HtmlReportWriter;
import com.intrigsoft.prathya.core.report.JsonReportWriter;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.TEST)
public class PrathyaReportMojo extends AbstractPrathyaMojo {

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

        } catch (PrathyaException e) {
            throw new MojoFailureException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write reports: " + e.getMessage(), e);
        }
    }
}
