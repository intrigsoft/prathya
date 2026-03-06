package dev.pratya.core.runner;

import dev.pratya.core.model.*;

import java.nio.file.Path;
import java.util.List;

public interface PratyaTestRunner {

    List<TestMethod> resolveTests(ModuleContract contract, List<TraceEntry> traces,
                                  RequirementStatus statusFilter, String requirementId);

    TestRunResult parseResults(Path surefireReportsDir, List<TestMethod> expectedTests);
}
