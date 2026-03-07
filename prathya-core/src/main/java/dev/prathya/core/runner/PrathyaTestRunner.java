package dev.prathya.core.runner;

import dev.prathya.core.model.*;

import java.nio.file.Path;
import java.util.List;

public interface PrathyaTestRunner {

    List<TestMethod> resolveTests(ModuleContract contract, List<TraceEntry> traces,
                                  RequirementStatus statusFilter, String requirementId);

    TestRunResult parseResults(Path surefireReportsDir, List<TestMethod> expectedTests);
}
