package dev.prathya.core.audit;

import dev.prathya.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAuditEngineTest {

    private DefaultAuditEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DefaultAuditEngine();
    }

    @Test
    void orphanedAnnotation() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED)
        );
        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-999")  // ID not in YAML
        );

        List<Violation> violations = engine.audit(contract, traces);

        assertEquals(1, violations.stream()
                .filter(v -> v.getType() == ViolationType.ORPHANED_ANNOTATION).count());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getRequirementId().equals("REQ-999")));
        assertEquals(Severity.ERROR, ViolationType.ORPHANED_ANNOTATION.getSeverity());
    }

    @Test
    void uncoveredRequirement() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED)
        );

        List<Violation> violations = engine.audit(contract, Collections.emptyList());

        assertEquals(1, violations.stream()
                .filter(v -> v.getType() == ViolationType.UNCOVERED_REQUIREMENT).count());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getRequirementId().equals("REQ-001")));
    }

    @Test
    void uncoveredCornerCase() {
        RequirementDefinition req = req("REQ-001", RequirementStatus.APPROVED,
                cc("REQ-001-CC-001"), cc("REQ-001-CC-002"));
        ModuleContract contract = buildContract(req);

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001")
                // REQ-001-CC-002 is not covered
        );

        List<Violation> violations = engine.audit(contract, traces);

        long uncoveredCCs = violations.stream()
                .filter(v -> v.getType() == ViolationType.UNCOVERED_CORNER_CASE).count();
        assertEquals(1, uncoveredCCs);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getCornerCaseId() != null && v.getCornerCaseId().equals("REQ-001-CC-002")));
        assertEquals(Severity.WARN, ViolationType.UNCOVERED_CORNER_CASE.getSeverity());
    }

    @Test
    void deprecatedReference() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.DEPRECATED)
        );
        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001")
        );

        List<Violation> violations = engine.audit(contract, traces);

        assertEquals(1, violations.stream()
                .filter(v -> v.getType() == ViolationType.DEPRECATED_REFERENCE).count());
        assertEquals(Severity.WARN, ViolationType.DEPRECATED_REFERENCE.getSeverity());
    }

    @Test
    void supersededReference() {
        RequirementDefinition req = req("REQ-001", RequirementStatus.SUPERSEDED);
        req.setSupersededBy("REQ-002");
        ModuleContract contract = buildContract(req);

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001")
        );

        List<Violation> violations = engine.audit(contract, traces);

        assertEquals(1, violations.stream()
                .filter(v -> v.getType() == ViolationType.SUPERSEDED_REFERENCE).count());
        assertTrue(violations.get(0).getMessage().contains("REQ-002"));
    }

    @Test
    void noViolationsWhenFullyCovered() {
        RequirementDefinition req = req("REQ-001", RequirementStatus.APPROVED,
                cc("REQ-001-CC-001"));
        ModuleContract contract = buildContract(req);

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001")
        );

        List<Violation> violations = engine.audit(contract, traces);
        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations.size());
    }

    @Test
    void draftRequirementNotFlagged() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.DRAFT)
        );

        List<Violation> violations = engine.audit(contract, Collections.emptyList());

        // Draft requirements should NOT trigger UNCOVERED_REQUIREMENT
        assertEquals(0, violations.stream()
                .filter(v -> v.getType() == ViolationType.UNCOVERED_REQUIREMENT).count());
    }

    @Test
    void deprecatedReference_notOrphaned() {
        // When a test references a deprecated requirement, the audit should produce
        // DEPRECATED_REFERENCE — NOT ORPHANED_ANNOTATION. This verifies that
        // the full contract (with deprecated reqs) is passed to the engine.
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.DEPRECATED),
                req("REQ-002", RequirementStatus.APPROVED)
        );
        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestB", "test2", "REQ-002")
        );

        List<Violation> violations = engine.audit(contract, traces);

        assertTrue(violations.stream().anyMatch(v -> v.getType() == ViolationType.DEPRECATED_REFERENCE),
                "Should detect DEPRECATED_REFERENCE");
        assertFalse(violations.stream().anyMatch(v -> v.getType() == ViolationType.ORPHANED_ANNOTATION),
                "Should NOT produce ORPHANED_ANNOTATION for deprecated requirement");
    }

    @Test
    void uncoveredCornerCase_withTestEnvironment_producesInfoViolation() {
        RequirementDefinition req = req("REQ-001", RequirementStatus.APPROVED,
                new CornerCase("REQ-001-CC-001", "Requires full server", TestEnvironment.FULL_SERVER),
                cc("REQ-001-CC-002"));
        ModuleContract contract = buildContract(req);

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001")
                // Both CC-001 and CC-002 are uncovered
        );

        List<Violation> violations = engine.audit(contract, traces);

        // CC-001 has testEnvironment=FULL_SERVER → UNCOVERED_CORNER_CASE_ENVIRONMENT (INFO)
        assertTrue(violations.stream().anyMatch(v ->
                        v.getType() == ViolationType.UNCOVERED_CORNER_CASE_ENVIRONMENT
                                && v.getCornerCaseId().equals("REQ-001-CC-001")),
                "Should produce UNCOVERED_CORNER_CASE_ENVIRONMENT for CC with testEnvironment");
        assertEquals(Severity.INFO, ViolationType.UNCOVERED_CORNER_CASE_ENVIRONMENT.getSeverity());

        // CC-002 has no testEnvironment → UNCOVERED_CORNER_CASE (WARN)
        assertTrue(violations.stream().anyMatch(v ->
                        v.getType() == ViolationType.UNCOVERED_CORNER_CASE
                                && v.getCornerCaseId().equals("REQ-001-CC-002")),
                "Should produce UNCOVERED_CORNER_CASE for CC without testEnvironment");
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
