package dev.pratya.core.coverage;

import dev.pratya.core.model.CoverageMatrix;
import dev.pratya.core.model.ModuleContract;
import dev.pratya.core.model.TestRunResult;
import dev.pratya.core.model.TraceEntry;

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
