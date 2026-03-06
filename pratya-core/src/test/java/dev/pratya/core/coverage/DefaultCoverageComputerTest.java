package dev.pratya.core.coverage;

import dev.pratya.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCoverageComputerTest {

    private DefaultCoverageComputer computer;

    @BeforeEach
    void setUp() {
        computer = new DefaultCoverageComputer();
    }

    @Test
    void fullCoverage() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED,
                        cc("REQ-001-CC-001"), cc("REQ-001-CC-002"))
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001"),
                trace("TestA", "test3", "REQ-001-CC-002")
        );

        CoverageMatrix matrix = computer.compute(contract, traces);

        assertEquals(1, matrix.getSummary().getActiveRequirements());
        assertEquals(1, matrix.getSummary().getCoveredRequirements());
        assertEquals(100.0, matrix.getSummary().getRequirementCoverage());
        assertEquals(2, matrix.getSummary().getTotalCornerCases());
        assertEquals(2, matrix.getSummary().getCoveredCornerCases());
        assertEquals(100.0, matrix.getSummary().getCornerCaseCoverage());
    }

    @Test
    void partialCoverage() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED,
                        cc("REQ-001-CC-001"), cc("REQ-001-CC-002")),
                req("REQ-002", RequirementStatus.APPROVED)
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001")
        );

        CoverageMatrix matrix = computer.compute(contract, traces);

        assertEquals(2, matrix.getSummary().getActiveRequirements());
        assertEquals(1, matrix.getSummary().getCoveredRequirements());
        assertEquals(50.0, matrix.getSummary().getRequirementCoverage());
        assertEquals(2, matrix.getSummary().getTotalCornerCases());
        assertEquals(1, matrix.getSummary().getCoveredCornerCases());
        assertEquals(50.0, matrix.getSummary().getCornerCaseCoverage());
    }

    @Test
    void zeroCoverage() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED, cc("REQ-001-CC-001"))
        );

        CoverageMatrix matrix = computer.compute(contract, Collections.emptyList());

        assertEquals(1, matrix.getSummary().getActiveRequirements());
        assertEquals(0, matrix.getSummary().getCoveredRequirements());
        assertEquals(0.0, matrix.getSummary().getRequirementCoverage());
    }

    @Test
    void deprecatedAndSupersededExcludedFromActiveCount() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED),
                req("REQ-002", RequirementStatus.DEPRECATED),
                req("REQ-003", RequirementStatus.SUPERSEDED)
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001")
        );

        CoverageMatrix matrix = computer.compute(contract, traces);

        assertEquals(3, matrix.getSummary().getTotalRequirements());
        assertEquals(1, matrix.getSummary().getActiveRequirements());
        assertEquals(1, matrix.getSummary().getCoveredRequirements());
        assertEquals(100.0, matrix.getSummary().getRequirementCoverage());
    }

    @Test
    void draftRequirementsCountAsActive() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.DRAFT)
        );

        CoverageMatrix matrix = computer.compute(contract, Collections.emptyList());

        assertEquals(1, matrix.getSummary().getActiveRequirements());
        assertEquals(0, matrix.getSummary().getCoveredRequirements());
    }

    // --- three-state coverage tests ---

    @Test
    void threeStateAllPassing() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED, cc("REQ-001-CC-001"))
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001")
        );

        TestRunResult testResults = new TestRunResult(List.of(
                new TestMethodResult("TestA", "test1", TestMethodResult.TestOutcome.PASSED, null),
                new TestMethodResult("TestA", "test2", TestMethodResult.TestOutcome.PASSED, null)
        ));

        CoverageMatrix matrix = computer.compute(contract, traces, testResults);

        RequirementCoverage reqCov = matrix.getRequirements().get(0);
        assertEquals(Boolean.TRUE, reqCov.getPassing());
        assertEquals(Boolean.TRUE, reqCov.getCornerCases().get(0).getPassing());
    }

    @Test
    void threeStateSomeFailing() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED, cc("REQ-001-CC-001"))
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001")
        );

        TestRunResult testResults = new TestRunResult(List.of(
                new TestMethodResult("TestA", "test1", TestMethodResult.TestOutcome.PASSED, null),
                new TestMethodResult("TestA", "test2", TestMethodResult.TestOutcome.FAILED, "assertion failed")
        ));

        CoverageMatrix matrix = computer.compute(contract, traces, testResults);

        RequirementCoverage reqCov = matrix.getRequirements().get(0);
        assertEquals(Boolean.TRUE, reqCov.getPassing()); // req itself has passing test
        assertEquals(Boolean.FALSE, reqCov.getCornerCases().get(0).getPassing()); // CC test failed
    }

    @Test
    void threeStateUncoveredKeepsNull() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED)
        );

        TestRunResult testResults = new TestRunResult(Collections.emptyList());
        CoverageMatrix matrix = computer.compute(contract, Collections.emptyList(), testResults);

        RequirementCoverage reqCov = matrix.getRequirements().get(0);
        assertNull(reqCov.getPassing());
    }

    @Test
    void threeStateNullTestResultsDelegatesToTwoArg() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED)
        );

        List<TraceEntry> traces = List.of(trace("TestA", "test1", "REQ-001"));

        CoverageMatrix matrix = computer.compute(contract, traces, null);

        RequirementCoverage reqCov = matrix.getRequirements().get(0);
        assertNull(reqCov.getPassing()); // two-arg always sets null
    }

    // --- helpers ---

    private ModuleContract buildContract(RequirementDefinition... reqs) {
        ModuleInfo module = new ModuleInfo();
        module.setId("TEST");
        module.setName("Test Module");
        return new ModuleContract(module, List.of(reqs));
    }

    private RequirementDefinition req(String id, RequirementStatus status, CornerCase... cornerCases) {
        RequirementDefinition r = new RequirementDefinition();
        r.setId(id);
        r.setTitle("Requirement " + id);
        r.setStatus(status);
        r.setCornerCases(List.of(cornerCases));
        return r;
    }

    private CornerCase cc(String id) {
        return new CornerCase(id, "Corner case " + id);
    }

    private TraceEntry trace(String className, String method, String... ids) {
        return new TraceEntry(className, method, List.of(ids));
    }
}
