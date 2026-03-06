package dev.pactum.core.audit;

import dev.pactum.core.model.ModuleContract;
import dev.pactum.core.model.TraceEntry;
import dev.pactum.core.model.Violation;

import java.util.List;

/**
 * Audits a contract against annotation traces and produces violations.
 */
public interface AuditEngine {

    List<Violation> audit(ModuleContract contract, List<TraceEntry> traces);
}
