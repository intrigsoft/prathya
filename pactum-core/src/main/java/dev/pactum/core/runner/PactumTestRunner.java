package dev.pactum.core.runner;

import dev.pactum.core.model.*;

import java.nio.file.Path;
import java.util.List;

public interface PactumTestRunner {

    List<TestMethod> resolveTests(ModuleContract contract, List<TraceEntry> traces,
                                  RequirementStatus statusFilter, String requirementId);

    TestRunResult parseResults(Path surefireReportsDir, List<TestMethod> expectedTests);
}
