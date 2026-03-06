package dev.pratya.core.audit;

import dev.pratya.core.model.ModuleContract;
import dev.pratya.core.model.TraceEntry;
import dev.pratya.core.model.Violation;

import java.util.List;

/**
 * Audits a contract against annotation traces and produces violations.
 */
public interface AuditEngine {

    List<Violation> audit(ModuleContract contract, List<TraceEntry> traces);
}
