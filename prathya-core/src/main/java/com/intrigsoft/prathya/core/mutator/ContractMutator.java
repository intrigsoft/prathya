package com.intrigsoft.prathya.core.mutator;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.model.ModuleContract;
import com.intrigsoft.prathya.core.model.RequirementDefinition;
import com.intrigsoft.prathya.core.model.TestEnvironment;

/**
 * Mutation operations on a {@link ModuleContract} with validation.
 * All methods modify the contract in place and return it for chaining.
 */
public interface ContractMutator {

    /**
     * Adds a new requirement to the contract.
     * Validates ID format, prefix, and uniqueness. Defaults status to DRAFT.
     */
    ModuleContract addRequirement(ModuleContract contract, RequirementDefinition req) throws PrathyaException;

    /**
     * Applies a partial update to an existing requirement.
     * Only non-null fields in the update are applied.
     */
    ModuleContract updateRequirement(ModuleContract contract, String id, RequirementUpdate update) throws PrathyaException;

    /**
     * Adds a corner case to an existing requirement.
     */
    ModuleContract addCornerCase(ModuleContract contract, String reqId, String ccId, String description) throws PrathyaException;

    /**
     * Adds a corner case with an optional test environment.
     */
    default ModuleContract addCornerCase(ModuleContract contract, String reqId, String ccId,
                                         String description, TestEnvironment testEnvironment) throws PrathyaException {
        return addCornerCase(contract, reqId, ccId, description);
    }

    /**
     * Updates the description of an existing corner case.
     */
    ModuleContract updateCornerCase(ModuleContract contract, String reqId, String ccId, String newDescription) throws PrathyaException;

    /**
     * Updates an existing corner case's description and/or test environment.
     */
    default ModuleContract updateCornerCase(ModuleContract contract, String reqId, String ccId,
                                            String newDescription, TestEnvironment testEnvironment) throws PrathyaException {
        return updateCornerCase(contract, reqId, ccId, newDescription);
    }

    /**
     * Deprecates an APPROVED requirement.
     */
    ModuleContract deprecateRequirement(ModuleContract contract, String id, String reason) throws PrathyaException;

    /**
     * Supersedes an old requirement with a new one.
     * The old requirement is marked SUPERSEDED with a link to the new one,
     * and the new requirement is created with a link back.
     */
    ModuleContract supersedeRequirement(ModuleContract contract, String oldId, RequirementDefinition newReq) throws PrathyaException;

    /**
     * Generates the next requirement ID (e.g. ORD-005 if the highest is ORD-004).
     */
    String generateNextRequirementId(ModuleContract contract);

    /**
     * Generates the next corner-case ID for a given requirement.
     */
    String generateNextCornerCaseId(ModuleContract contract, String reqId) throws PrathyaException;
}
