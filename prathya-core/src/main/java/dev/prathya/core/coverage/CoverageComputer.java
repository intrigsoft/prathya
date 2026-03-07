package dev.prathya.core.coverage;

import dev.prathya.core.model.CoverageMatrix;
import dev.prathya.core.model.ModuleContract;
import dev.prathya.core.model.TestRunResult;
import dev.prathya.core.model.TraceEntry;

import java.util.List;

/**
 * Computes requirement coverage by cross-referencing a contract against annotation traces.
 */
public interface CoverageComputer {

    CoverageMatrix compute(ModuleContract contract, List<TraceEntry> traces);

    default CoverageMatrix compute(ModuleContract contract, List<TraceEntry> traces, TestRunResult testResults) {
        return compute(contract, traces);
    }
}
