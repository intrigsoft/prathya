package com.intrigsoft.prathya.core.audit;

import com.intrigsoft.prathya.core.model.ModuleContract;
import com.intrigsoft.prathya.core.model.TraceEntry;
import com.intrigsoft.prathya.core.model.Violation;

import java.util.List;

/**
 * Audits a contract against annotation traces and produces violations.
 */
public interface AuditEngine {

    List<Violation> audit(ModuleContract contract, List<TraceEntry> traces);
}
