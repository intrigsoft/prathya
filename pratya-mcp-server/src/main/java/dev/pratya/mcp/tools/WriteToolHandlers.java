package dev.pratya.mcp.tools;

import dev.pratya.core.PratyaException;
import dev.pratya.core.model.*;
import dev.pratya.core.mutator.ContractMutator;
import dev.pratya.core.mutator.DefaultContractMutator;
import dev.pratya.core.mutator.RequirementUpdate;
import dev.pratya.core.parser.ContractWriter;
import dev.pratya.core.parser.RequirementParser;
import dev.pratya.core.parser.YamlContractWriter;
import dev.pratya.core.parser.YamlRequirementParser;
import dev.pratya.mcp.PratyaServerConfig;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

import static dev.pratya.mcp.tools.ReadToolHandlers.errorResult;
import static dev.pratya.mcp.tools.ReadToolHandlers.textResult;

/**
 * Handlers for the 6 write MCP tools.
 * Each handler follows: load → mutate → save → return success message.
 */
public class WriteToolHandlers {

    private final PratyaServerConfig config;
    private final RequirementParser parser = new YamlRequirementParser();
    private final ContractWriter writer = new YamlContractWriter();
    private final ContractMutator mutator = new DefaultContractMutator();

    public WriteToolHandlers(PratyaServerConfig config) {
        this.config = config;
    }

    // ── add_requirement ──

    public McpSchema.CallToolResult addRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();

            RequirementDefinition req = new RequirementDefinition();
            req.setTitle(requireStringArg(args, "title"));
            String id = stringArg(args, "id");
            if (id != null) req.setId(id);
            req.setDescription(stringArg(args, "description"));

            String status = stringArg(args, "status");
            if (status != null) {
                req.setStatus(RequirementStatus.valueOf(status.toUpperCase()));
            }

            Object criteria = args.get("acceptance_criteria");
            if (criteria instanceof List<?> list) {
                req.setAcceptanceCriteria(list.stream().map(Object::toString).toList());
            }

            mutator.addRequirement(contract, req);
            saveContract(contract);

            return textResult("Added requirement " + req.getId() + " (" + req.getTitle() + ")");
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── update_requirement ──

    public McpSchema.CallToolResult updateRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
            String id = requireStringArg(args, "id");

            RequirementUpdate update = new RequirementUpdate();
            update.setTitle(stringArg(args, "title"));
            update.setDescription(stringArg(args, "description"));
            update.setVersion(stringArg(args, "version"));
            update.setChangelogNote(stringArg(args, "note"));

            Object criteria = args.get("acceptance_criteria");
            if (criteria instanceof List<?> list) {
                update.setAcceptanceCriteria(list.stream().map(Object::toString).toList());
            }

            mutator.updateRequirement(contract, id, update);
            saveContract(contract);

            return textResult("Updated requirement " + id);
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── add_corner_case ──

    public McpSchema.CallToolResult addCornerCase(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
            String reqId = requireStringArg(args, "req_id");
            String description = requireStringArg(args, "description");
            String ccId = stringArg(args, "id");

            mutator.addCornerCase(contract, reqId, ccId, description);
            saveContract(contract);

            // Find the newly added CC to report its ID
            RequirementDefinition req = contract.getRequirements().stream()
                    .filter(r -> r.getId().equals(reqId)).findFirst().orElseThrow();
            CornerCase added = req.getCornerCases().get(req.getCornerCases().size() - 1);
            return textResult("Added corner case " + added.getId() + " to " + reqId);
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── update_corner_case ──

    public McpSchema.CallToolResult updateCornerCase(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
            String reqId = requireStringArg(args, "req_id");
            String ccId = requireStringArg(args, "cc_id");
            String description = requireStringArg(args, "description");

            mutator.updateCornerCase(contract, reqId, ccId, description);
            saveContract(contract);

            return textResult("Updated corner case " + ccId);
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── deprecate_requirement ──

    public McpSchema.CallToolResult deprecateRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
            String id = requireStringArg(args, "id");
            String reason = stringArg(args, "reason");

            mutator.deprecateRequirement(contract, id, reason);
            saveContract(contract);

            return textResult("Deprecated requirement " + id);
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── supersede_requirement ──

    public McpSchema.CallToolResult supersedeRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
            String oldId = requireStringArg(args, "old_id");
            String title = requireStringArg(args, "title");

            RequirementDefinition newReq = new RequirementDefinition();
            newReq.setTitle(title);
            String newId = stringArg(args, "new_id");
            if (newId != null) newReq.setId(newId);
            newReq.setDescription(stringArg(args, "description"));

            Object criteria = args.get("acceptance_criteria");
            if (criteria instanceof List<?> list) {
                newReq.setAcceptanceCriteria(list.stream().map(Object::toString).toList());
            }

            mutator.supersedeRequirement(contract, oldId, newReq);
            saveContract(contract);

            return textResult("Superseded " + oldId + " with " + newReq.getId() + " (" + title + ")");
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── helpers ──

    private ModuleContract loadContract() throws PratyaException {
        return parser.parse(config.getContractFile());
    }

    private void saveContract(ModuleContract contract) throws PratyaException {
        writer.write(contract, config.getContractFile());
    }

    private static String stringArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }

    private static String requireStringArg(Map<String, Object> args, String key) throws PratyaException {
        String v = stringArg(args, key);
        if (v == null || v.isBlank()) {
            throw new PratyaException("Missing required argument: " + key);
        }
        return v;
    }
}
