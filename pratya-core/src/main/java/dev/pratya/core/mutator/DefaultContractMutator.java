package dev.pratya.core.mutator;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ContractMutator}.
 */
public class DefaultContractMutator implements ContractMutator {

    @Override
    public ModuleContract addRequirement(ModuleContract contract, RequirementDefinition req) throws PratyaException {
        String moduleId = contract.getModule().getId();

        if (req.getId() == null) {
            req.setId(generateNextRequirementId(contract));
        }
        validateReqIdFormat(req.getId(), moduleId);
        validateReqIdPrefix(req.getId(), moduleId);
        ensureUniqueReqId(contract, req.getId());

        if (req.getStatus() == null) {
            req.setStatus(RequirementStatus.DRAFT);
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new PratyaException("Requirement title is required");
        }

        contract.getRequirements().add(req);
        return contract;
    }

    @Override
    public ModuleContract updateRequirement(ModuleContract contract, String id, RequirementUpdate update) throws PratyaException {
        RequirementDefinition req = findRequirement(contract, id);

        if (update.getTitle() != null) {
            req.setTitle(update.getTitle());
        }
        if (update.getDescription() != null) {
            req.setDescription(update.getDescription());
        }
        if (update.getVersion() != null) {
            req.setVersion(update.getVersion());
        }
        if (update.getAcceptanceCriteria() != null) {
            req.setAcceptanceCriteria(update.getAcceptanceCriteria());
        }
        if (update.getChangelogNote() != null) {
            ChangelogEntry entry = new ChangelogEntry(
                    update.getVersion() != null ? update.getVersion() : req.getVersion(),
                    LocalDate.now(),
                    update.getChangelogNote()
            );
            if (req.getChangelog() == null) {
                req.setChangelog(new ArrayList<>());
            }
            req.getChangelog().add(entry);
        }

        return contract;
    }

    @Override
    public ModuleContract addCornerCase(ModuleContract contract, String reqId, String ccId, String description) throws PratyaException {
        RequirementDefinition req = findRequirement(contract, reqId);
        String moduleId = contract.getModule().getId();

        if (ccId == null) {
            ccId = generateNextCornerCaseId(contract, reqId);
        }
        validateCcIdFormat(ccId, moduleId);
        validateCcIdPrefix(ccId, reqId);
        ensureUniqueCcId(req, ccId);

        if (description == null || description.isBlank()) {
            throw new PratyaException("Corner case description is required");
        }

        if (req.getCornerCases() == null) {
            req.setCornerCases(new ArrayList<>());
        }
        req.getCornerCases().add(new CornerCase(ccId, description));
        return contract;
    }

    @Override
    public ModuleContract updateCornerCase(ModuleContract contract, String reqId, String ccId, String newDescription) throws PratyaException {
        RequirementDefinition req = findRequirement(contract, reqId);
        CornerCase cc = findCornerCase(req, ccId);
        cc.setDescription(newDescription);
        return contract;
    }

    @Override
    public ModuleContract deprecateRequirement(ModuleContract contract, String id, String reason) throws PratyaException {
        RequirementDefinition req = findRequirement(contract, id);

        if (req.getStatus() != RequirementStatus.APPROVED) {
            throw new PratyaException("Only APPROVED requirements can be deprecated, but "
                    + id + " is " + req.getStatus());
        }

        req.setStatus(RequirementStatus.DEPRECATED);

        ChangelogEntry entry = new ChangelogEntry(
                req.getVersion(),
                LocalDate.now(),
                reason != null ? "Deprecated: " + reason : "Deprecated"
        );
        if (req.getChangelog() == null) {
            req.setChangelog(new ArrayList<>());
        }
        req.getChangelog().add(entry);

        return contract;
    }

    @Override
    public ModuleContract supersedeRequirement(ModuleContract contract, String oldId, RequirementDefinition newReq) throws PratyaException {
        RequirementDefinition oldReq = findRequirement(contract, oldId);

        if (oldReq.getStatus() != RequirementStatus.APPROVED) {
            throw new PratyaException("Only APPROVED requirements can be superseded, but "
                    + oldId + " is " + oldReq.getStatus());
        }

        // Set up the new requirement first so its ID is available
        if (newReq.getId() == null) {
            newReq.setId(generateNextRequirementId(contract));
        }
        String moduleId = contract.getModule().getId();
        validateReqIdFormat(newReq.getId(), moduleId);
        validateReqIdPrefix(newReq.getId(), moduleId);
        ensureUniqueReqId(contract, newReq.getId());

        if (newReq.getStatus() == null) {
            newReq.setStatus(RequirementStatus.DRAFT);
        }
        if (newReq.getTitle() == null || newReq.getTitle().isBlank()) {
            throw new PratyaException("Replacement requirement title is required");
        }
        newReq.setSupersedes(oldId);

        // Mark old as superseded
        oldReq.setStatus(RequirementStatus.SUPERSEDED);
        oldReq.setSupersededBy(newReq.getId());

        ChangelogEntry entry = new ChangelogEntry(
                oldReq.getVersion(),
                LocalDate.now(),
                "Superseded by " + newReq.getId()
        );
        if (oldReq.getChangelog() == null) {
            oldReq.setChangelog(new ArrayList<>());
        }
        oldReq.getChangelog().add(entry);

        contract.getRequirements().add(newReq);
        return contract;
    }

    @Override
    public String generateNextRequirementId(ModuleContract contract) {
        String prefix = contract.getModule().getId();
        int maxSeq = 0;
        for (RequirementDefinition req : contract.getRequirements()) {
            String id = req.getId();
            if (id != null && id.startsWith(prefix + "-")) {
                String seqStr = id.substring(prefix.length() + 1);
                try {
                    int seq = Integer.parseInt(seqStr);
                    maxSeq = Math.max(maxSeq, seq);
                } catch (NumberFormatException ignored) {}
            }
        }
        return prefix + "-" + String.format("%03d", maxSeq + 1);
    }

    @Override
    public String generateNextCornerCaseId(ModuleContract contract, String reqId) throws PratyaException {
        RequirementDefinition req = findRequirement(contract, reqId);
        int maxSeq = 0;
        String ccPrefix = reqId + "-CC-";
        if (req.getCornerCases() != null) {
            for (CornerCase cc : req.getCornerCases()) {
                String id = cc.getId();
                if (id != null && id.startsWith(ccPrefix)) {
                    String seqStr = id.substring(ccPrefix.length());
                    try {
                        int seq = Integer.parseInt(seqStr);
                        maxSeq = Math.max(maxSeq, seq);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return ccPrefix + String.format("%03d", maxSeq + 1);
    }

    // ── helpers ──

    private RequirementDefinition findRequirement(ModuleContract contract, String id) throws PratyaException {
        for (RequirementDefinition req : contract.getRequirements()) {
            if (req.getId().equals(id)) {
                return req;
            }
        }
        throw new PratyaException("Requirement not found: " + id);
    }

    private CornerCase findCornerCase(RequirementDefinition req, String ccId) throws PratyaException {
        if (req.getCornerCases() != null) {
            for (CornerCase cc : req.getCornerCases()) {
                if (cc.getId().equals(ccId)) {
                    return cc;
                }
            }
        }
        throw new PratyaException("Corner case not found: " + ccId + " in requirement " + req.getId());
    }

    private void validateReqIdFormat(String id, String moduleId) throws PratyaException {
        if (!ContractConstants.REQ_ID_PATTERN.matcher(id).matches()) {
            throw new PratyaException("Invalid requirement ID format: '" + id
                    + "'. Expected {MODULE}-{SEQ} (e.g. " + moduleId + "-001)");
        }
    }

    private void validateReqIdPrefix(String id, String moduleId) throws PratyaException {
        if (!id.startsWith(moduleId + "-")) {
            throw new PratyaException("Requirement ID '" + id
                    + "' must start with module prefix '" + moduleId + "-'");
        }
    }

    private void ensureUniqueReqId(ModuleContract contract, String id) throws PratyaException {
        for (RequirementDefinition req : contract.getRequirements()) {
            if (req.getId().equals(id)) {
                throw new PratyaException("Duplicate requirement ID: " + id);
            }
        }
    }

    private void validateCcIdFormat(String id, String moduleId) throws PratyaException {
        if (!ContractConstants.CC_ID_PATTERN.matcher(id).matches()) {
            throw new PratyaException("Invalid corner case ID format: '" + id
                    + "'. Expected {MODULE}-{SEQ}-CC-{N} (e.g. " + moduleId + "-001-CC-001)");
        }
    }

    private void validateCcIdPrefix(String ccId, String reqId) throws PratyaException {
        if (!ccId.startsWith(reqId + "-CC-")) {
            throw new PratyaException("Corner case ID '" + ccId
                    + "' must start with '" + reqId + "-CC-'");
        }
    }

    private void ensureUniqueCcId(RequirementDefinition req, String ccId) throws PratyaException {
        if (req.getCornerCases() != null) {
            for (CornerCase cc : req.getCornerCases()) {
                if (cc.getId().equals(ccId)) {
                    throw new PratyaException("Duplicate corner case ID: " + ccId);
                }
            }
        }
    }
}
