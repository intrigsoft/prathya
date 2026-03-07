package dev.pratya.core.mutator;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.ModuleContract;
import dev.pratya.core.model.RequirementDefinition;

/**
 * Mutation operations on a {@link ModuleContract} with validation.
 * All methods modify the contract in place and return it for chaining.
 */
public interface ContractMutator {

    /**
     * Adds a new requirement to the contract.
     * Validates ID format, prefix, and uniqueness. Defaults status to DRAFT.
     */
    ModuleContract addRequirement(ModuleContract contract, RequirementDefinition req) throws PratyaException;

    /**
     * Applies a partial update to an existing requirement.
     * Only non-null fields in the update are applied.
     */
    ModuleContract updateRequirement(ModuleContract contract, String id, RequirementUpdate update) throws PratyaException;

    /**
     * Adds a corner case to an existing requirement.
     */
    ModuleContract addCornerCase(ModuleContract contract, String reqId, String ccId, String description) throws PratyaException;

    /**
     * Updates the description of an existing corner case.
     */
    ModuleContract updateCornerCase(ModuleContract contract, String reqId, String ccId, String newDescription) throws PratyaException;

    /**
     * Deprecates an APPROVED requirement.
     */
    ModuleContract deprecateRequirement(ModuleContract contract, String id, String reason) throws PratyaException;

    /**
     * Supersedes an old requirement with a new one.
     * The old requirement is marked SUPERSEDED with a link to the new one,
     * and the new requirement is created with a link back.
     */
    ModuleContract supersedeRequirement(ModuleContract contract, String oldId, RequirementDefinition newReq) throws PratyaException;

    /**
     * Generates the next requirement ID (e.g. ORD-005 if the highest is ORD-004).
     */
    String generateNextRequirementId(ModuleContract contract);

    /**
     * Generates the next corner-case ID for a given requirement.
     */
    String generateNextCornerCaseId(ModuleContract contract, String reqId) throws PratyaException;
}
