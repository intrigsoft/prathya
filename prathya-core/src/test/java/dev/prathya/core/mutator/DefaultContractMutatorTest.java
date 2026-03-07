package dev.prathya.core.mutator;

import dev.prathya.core.PrathyaException;
import dev.prathya.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultContractMutatorTest {

    private final DefaultContractMutator mutator = new DefaultContractMutator();
    private ModuleContract contract;

    @BeforeEach
    void setUp() {
        ModuleInfo module = new ModuleInfo();
        module.setId("ORD");
        module.setName("Order Service");

        RequirementDefinition req1 = new RequirementDefinition();
        req1.setId("ORD-001");
        req1.setTitle("Create order");
        req1.setStatus(RequirementStatus.APPROVED);
        req1.setVersion("1.0.0");
        req1.setCornerCases(new ArrayList<>(List.of(
                new CornerCase("ORD-001-CC-001", "Empty list")
        )));

        RequirementDefinition req2 = new RequirementDefinition();
        req2.setId("ORD-002");
        req2.setTitle("Cancel order");
        req2.setStatus(RequirementStatus.APPROVED);
        req2.setVersion("1.0.0");

        contract = new ModuleContract(module, new ArrayList<>(List.of(req1, req2)));
    }

    // ── addRequirement ──

    @Test
    void addRequirement_withExplicitId() throws Exception {
        RequirementDefinition req = new RequirementDefinition();
        req.setId("ORD-003");
        req.setTitle("Ship order");

        mutator.addRequirement(contract, req);

        assertEquals(3, contract.getRequirements().size());
        assertEquals("ORD-003", contract.getRequirements().get(2).getId());
        assertEquals(RequirementStatus.DRAFT, contract.getRequirements().get(2).getStatus());
    }

    @Test
    void addRequirement_autoId() throws Exception {
        RequirementDefinition req = new RequirementDefinition();
        req.setTitle("Ship order");

        mutator.addRequirement(contract, req);

        assertEquals("ORD-003", req.getId());
    }

    @Test
    void addRequirement_duplicateId_throws() {
        RequirementDefinition req = new RequirementDefinition();
        req.setId("ORD-001");
        req.setTitle("Duplicate");

        PrathyaException ex = assertThrows(PrathyaException.class,
                () -> mutator.addRequirement(contract, req));
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    void addRequirement_wrongPrefix_throws() {
        RequirementDefinition req = new RequirementDefinition();
        req.setId("USR-001");
        req.setTitle("Wrong module");

        PrathyaException ex = assertThrows(PrathyaException.class,
                () -> mutator.addRequirement(contract, req));
        assertTrue(ex.getMessage().contains("prefix"));
    }

    @Test
    void addRequirement_invalidFormat_throws() {
        RequirementDefinition req = new RequirementDefinition();
        req.setId("ord-001");
        req.setTitle("lowercase");

        assertThrows(PrathyaException.class,
                () -> mutator.addRequirement(contract, req));
    }

    @Test
    void addRequirement_missingTitle_throws() {
        RequirementDefinition req = new RequirementDefinition();
        req.setId("ORD-003");

        PrathyaException ex = assertThrows(PrathyaException.class,
                () -> mutator.addRequirement(contract, req));
        assertTrue(ex.getMessage().contains("title"));
    }

    // ── updateRequirement ──

    @Test
    void updateRequirement_appliesPartialUpdate() throws Exception {
        RequirementUpdate update = new RequirementUpdate();
        update.setTitle("Create order v2");
        update.setAcceptanceCriteria(List.of("New criterion"));
        update.setChangelogNote("Updated title");

        mutator.updateRequirement(contract, "ORD-001", update);

        RequirementDefinition req = contract.getRequirements().get(0);
        assertEquals("Create order v2", req.getTitle());
        assertEquals(List.of("New criterion"), req.getAcceptanceCriteria());
        assertFalse(req.getChangelog().isEmpty());
        assertEquals("Updated title", req.getChangelog().get(req.getChangelog().size() - 1).getNote());
    }

    @Test
    void updateRequirement_nullFieldsUnchanged() throws Exception {
        RequirementUpdate update = new RequirementUpdate();
        update.setTitle("New title");
        // description left null — should not change

        mutator.updateRequirement(contract, "ORD-001", update);

        RequirementDefinition req = contract.getRequirements().get(0);
        assertEquals("New title", req.getTitle());
        // description was already null, should remain so
    }

    @Test
    void updateRequirement_notFound_throws() {
        RequirementUpdate update = new RequirementUpdate();
        update.setTitle("Nope");

        assertThrows(PrathyaException.class,
                () -> mutator.updateRequirement(contract, "ORD-999", update));
    }

    // ── addCornerCase ──

    @Test
    void addCornerCase_withExplicitId() throws Exception {
        mutator.addCornerCase(contract, "ORD-001", "ORD-001-CC-002", "Negative quantity");

        assertEquals(2, contract.getRequirements().get(0).getCornerCases().size());
        assertEquals("ORD-001-CC-002",
                contract.getRequirements().get(0).getCornerCases().get(1).getId());
    }

    @Test
    void addCornerCase_autoId() throws Exception {
        mutator.addCornerCase(contract, "ORD-001", null, "Auto ID test");

        CornerCase added = contract.getRequirements().get(0).getCornerCases().get(1);
        assertEquals("ORD-001-CC-002", added.getId());
    }

    @Test
    void addCornerCase_duplicateId_throws() {
        assertThrows(PrathyaException.class,
                () -> mutator.addCornerCase(contract, "ORD-001", "ORD-001-CC-001", "Duplicate"));
    }

    @Test
    void addCornerCase_wrongPrefix_throws() {
        assertThrows(PrathyaException.class,
                () -> mutator.addCornerCase(contract, "ORD-001", "ORD-002-CC-001", "Wrong req prefix"));
    }

    @Test
    void addCornerCase_missingDescription_throws() {
        assertThrows(PrathyaException.class,
                () -> mutator.addCornerCase(contract, "ORD-001", "ORD-001-CC-002", null));
    }

    // ── updateCornerCase ──

    @Test
    void updateCornerCase_success() throws Exception {
        mutator.updateCornerCase(contract, "ORD-001", "ORD-001-CC-001", "Updated description");

        assertEquals("Updated description",
                contract.getRequirements().get(0).getCornerCases().get(0).getDescription());
    }

    @Test
    void updateCornerCase_notFound_throws() {
        assertThrows(PrathyaException.class,
                () -> mutator.updateCornerCase(contract, "ORD-001", "ORD-001-CC-999", "Nope"));
    }

    @Test
    void addCornerCase_withTestEnvironment() throws Exception {
        mutator.addCornerCase(contract, "ORD-001", "ORD-001-CC-002",
                "Requires full server", TestEnvironment.FULL_SERVER);

        CornerCase added = contract.getRequirements().get(0).getCornerCases().get(1);
        assertEquals("ORD-001-CC-002", added.getId());
        assertEquals("Requires full server", added.getDescription());
        assertEquals(TestEnvironment.FULL_SERVER, added.getTestEnvironment());
    }

    @Test
    void updateCornerCase_withTestEnvironment() throws Exception {
        mutator.updateCornerCase(contract, "ORD-001", "ORD-001-CC-001",
                null, TestEnvironment.INTEGRATION);

        CornerCase cc = contract.getRequirements().get(0).getCornerCases().get(0);
        assertEquals("Empty list", cc.getDescription()); // unchanged
        assertEquals(TestEnvironment.INTEGRATION, cc.getTestEnvironment());
    }

    // ── deprecateRequirement ──

    @Test
    void deprecateRequirement_fromApproved() throws Exception {
        mutator.deprecateRequirement(contract, "ORD-001", "No longer needed");

        RequirementDefinition req = contract.getRequirements().get(0);
        assertEquals(RequirementStatus.DEPRECATED, req.getStatus());
        assertTrue(req.getChangelog().get(req.getChangelog().size() - 1).getNote().contains("Deprecated"));
    }

    @Test
    void deprecateRequirement_fromDraft_throws() {
        contract.getRequirements().get(0).setStatus(RequirementStatus.DRAFT);

        PrathyaException ex = assertThrows(PrathyaException.class,
                () -> mutator.deprecateRequirement(contract, "ORD-001", "reason"));
        assertTrue(ex.getMessage().contains("APPROVED"));
    }

    // ── supersedeRequirement ──

    @Test
    void supersedeRequirement_success() throws Exception {
        RequirementDefinition newReq = new RequirementDefinition();
        newReq.setTitle("Create order v2");

        mutator.supersedeRequirement(contract, "ORD-001", newReq);

        RequirementDefinition oldReq = contract.getRequirements().get(0);
        assertEquals(RequirementStatus.SUPERSEDED, oldReq.getStatus());
        assertEquals("ORD-003", oldReq.getSupersededBy());

        assertEquals("ORD-003", newReq.getId());
        assertEquals("ORD-001", newReq.getSupersedes());
        assertEquals(RequirementStatus.DRAFT, newReq.getStatus());
        assertEquals(3, contract.getRequirements().size());
    }

    @Test
    void supersedeRequirement_fromDraft_throws() {
        contract.getRequirements().get(0).setStatus(RequirementStatus.DRAFT);

        RequirementDefinition newReq = new RequirementDefinition();
        newReq.setTitle("V2");

        assertThrows(PrathyaException.class,
                () -> mutator.supersedeRequirement(contract, "ORD-001", newReq));
    }

    // ── ID generation ──

    @Test
    void generateNextRequirementId() {
        assertEquals("ORD-003", mutator.generateNextRequirementId(contract));
    }

    @Test
    void generateNextCornerCaseId() throws Exception {
        assertEquals("ORD-001-CC-002", mutator.generateNextCornerCaseId(contract, "ORD-001"));
    }

    @Test
    void generateNextCornerCaseId_noExisting() throws Exception {
        assertEquals("ORD-002-CC-001", mutator.generateNextCornerCaseId(contract, "ORD-002"));
    }
}
