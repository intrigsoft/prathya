package com.intrigsoft.prathya.core.coverage;

import com.intrigsoft.prathya.core.model.CoverageMatrix;
import com.intrigsoft.prathya.core.model.ModuleContract;
import com.intrigsoft.prathya.core.model.TestRunResult;
import com.intrigsoft.prathya.core.model.TraceEntry;

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
