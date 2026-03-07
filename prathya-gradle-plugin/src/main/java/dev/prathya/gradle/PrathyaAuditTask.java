package dev.prathya.gradle;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.model.Severity;
import dev.prathya.core.model.Violation;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public abstract class PrathyaAuditTask extends AbstractPrathyaTask {

    @TaskAction
    public void audit() {
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
                getLogger().lifecycle("  {} {} — {}: {}",
                        prefix, v.getType().name(), location, v.getMessage());
            }

            // Log summary
            logSummary(result);

        } catch (PrathyaException e) {
            throw new GradleException("Failed to parse CONTRACT.yaml: " + e.getMessage(), e);
        }
    }
}
