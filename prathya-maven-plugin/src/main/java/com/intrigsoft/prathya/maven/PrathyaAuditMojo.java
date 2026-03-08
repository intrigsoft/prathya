package com.intrigsoft.prathya.maven;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.model.Severity;
import com.intrigsoft.prathya.core.model.Violation;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "audit", threadSafe = true)
public class PrathyaAuditMojo extends AbstractPrathyaMojo {

    @Override
    public void execute() throws MojoFailureException {
        if (shouldSkip()) {
            return;
        }

        try {
            PipelineResult result = runPipeline();

            // Log each violation with severity prefix
            for (Violation v : result.violations()) {
                String prefix = v.getType().getSeverity() == Severity.ERROR ? "[ERROR]" : "[WARN]";
                String location = v.getCornerCaseId() != null
                        ? v.getRequirementId() + " / " + v.getCornerCaseId()
                        : v.getRequirementId() != null ? v.getRequirementId() : "(global)";
                getLog().info(String.format("  %s %s — %s: %s",
                        prefix, v.getType().name(), location, v.getMessage()));
            }

            // Log summary
            logSummary(result);

        } catch (PrathyaException e) {
            throw new MojoFailureException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        }
    }
}
