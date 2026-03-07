package dev.prathya.core.audit;

import dev.prathya.core.model.ModuleContract;
import dev.prathya.core.model.TraceEntry;
import dev.prathya.core.model.Violation;

import java.util.List;

/**
 * Audits a contract against annotation traces and produces violations.
 */
public interface AuditEngine {

    List<Violation> audit(ModuleContract contract, List<TraceEntry> traces);
}
