package com.intrigsoft.prathya.mcp.tools;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.model.*;
import com.intrigsoft.prathya.core.model.TestEnvironment;
import com.intrigsoft.prathya.core.mutator.ContractMutator;
import com.intrigsoft.prathya.core.mutator.DefaultContractMutator;
import com.intrigsoft.prathya.core.mutator.RequirementUpdate;
import com.intrigsoft.prathya.core.parser.ContractWriter;
import com.intrigsoft.prathya.core.parser.RequirementParser;
import com.intrigsoft.prathya.core.parser.YamlContractWriter;
import com.intrigsoft.prathya.core.parser.YamlRequirementParser;
import com.intrigsoft.prathya.mcp.PrathyaServerConfig;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.intrigsoft.prathya.mcp.tools.ReadToolHandlers.errorResult;
import static com.intrigsoft.prathya.mcp.tools.ReadToolHandlers.textResult;

/**
 * Handlers for the 6 write MCP tools.
 * Each handler follows: load → mutate → save → return success message.
 */
public class WriteToolHandlers {

    private final PrathyaServerConfig config;
    private final RequirementParser parser = new YamlRequirementParser();
    private final ContractWriter writer = new YamlContractWriter();
    private final ContractMutator mutator = new DefaultContractMutator();

    public WriteToolHandlers(PrathyaServerConfig config) {
        this.config = config;
    }

    // ── add_requirement ──

    public McpSchema.CallToolResult addRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);

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
            saveContract(contract, args);

            return textResult("Added requirement " + req.getId() + " (" + req.getTitle() + ")");
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── update_requirement ──

    public McpSchema.CallToolResult updateRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
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
            saveContract(contract, args);

            return textResult("Updated requirement " + id);
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── add_corner_case ──

    public McpSchema.CallToolResult addCornerCase(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
            String reqId = requireStringArg(args, "req_id");
            String description = requireStringArg(args, "description");
            String ccId = stringArg(args, "id");
            String envStr = stringArg(args, "test_environment");
            TestEnvironment env = envStr != null ? TestEnvironment.fromYaml(envStr) : null;

            mutator.addCornerCase(contract, reqId, ccId, description, env);
            saveContract(contract, args);

            // Find the newly added CC to report its ID
            RequirementDefinition req = contract.getRequirements().stream()
                    .filter(r -> r.getId().equals(reqId)).findFirst().orElseThrow();
            CornerCase added = req.getCornerCases().get(req.getCornerCases().size() - 1);
            return textResult("Added corner case " + added.getId() + " to " + reqId);
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── update_corner_case ──

    public McpSchema.CallToolResult updateCornerCase(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
            String reqId = requireStringArg(args, "req_id");
            String ccId = requireStringArg(args, "cc_id");
            String description = stringArg(args, "description");
            String envStr = stringArg(args, "test_environment");
            TestEnvironment env = envStr != null ? TestEnvironment.fromYaml(envStr) : null;

            mutator.updateCornerCase(contract, reqId, ccId, description, env);
            saveContract(contract, args);

            return textResult("Updated corner case " + ccId);
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── deprecate_requirement ──

    public McpSchema.CallToolResult deprecateRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
            String id = requireStringArg(args, "id");
            String reason = stringArg(args, "reason");

            mutator.deprecateRequirement(contract, id, reason);
            saveContract(contract, args);

            return textResult("Deprecated requirement " + id);
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── supersede_requirement ──

    public McpSchema.CallToolResult supersedeRequirement(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
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
            saveContract(contract, args);

            return textResult("Superseded " + oldId + " with " + newReq.getId() + " (" + title + ")");
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── helpers ──

    private ModuleContract loadContract(Map<String, Object> args) throws PrathyaException {
        return parser.parse(resolveContractFile(args));
    }

    private void saveContract(ModuleContract contract, Map<String, Object> args) throws PrathyaException {
        writer.write(contract, resolveContractFile(args));
    }

    private Path resolveContractFile(Map<String, Object> args) {
        String override = stringArg(args, "contract_file");
        if (override != null) {
            return Path.of(override);
        }
        return config.getContractFile();
    }

    private static String stringArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }

    private static String requireStringArg(Map<String, Object> args, String key) throws PrathyaException {
        String v = stringArg(args, key);
        if (v == null || v.isBlank()) {
            throw new PrathyaException("Missing required argument: " + key);
        }
        return v;
    }
}
