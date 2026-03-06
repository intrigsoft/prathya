package dev.pactum.core.coverage;

import dev.pactum.core.model.CoverageMatrix;
import dev.pactum.core.model.ModuleContract;
import dev.pactum.core.model.TraceEntry;

import java.util.List;

/**
 * Computes requirement coverage by cross-referencing a contract against annotation traces.
 */
public interface CoverageComputer {

    CoverageMatrix compute(ModuleContract contract, List<TraceEntry> traces);
}
