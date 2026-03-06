package dev.pactum.maven;

import dev.pactum.core.PactumException;
import dev.pactum.core.model.Severity;
import dev.pactum.core.model.Violation;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "audit", threadSafe = true)
public class PactumAuditMojo extends AbstractPactumMojo {

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

        } catch (PactumException e) {
            throw new MojoFailureException("Failed to parse REQUIREMENT.yaml: " + e.getMessage(), e);
        }
    }
}
