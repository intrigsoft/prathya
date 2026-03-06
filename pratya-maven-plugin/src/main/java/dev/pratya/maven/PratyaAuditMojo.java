package dev.pratya.maven;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.Severity;
import dev.pratya.core.model.Violation;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "audit", threadSafe = true)
public class PratyaAuditMojo extends AbstractPratyaMojo {

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

        } catch (PratyaException e) {
            throw new MojoFailureException("Failed to parse REQUIREMENT.yaml: " + e.getMessage(), e);
        }
    }
}
