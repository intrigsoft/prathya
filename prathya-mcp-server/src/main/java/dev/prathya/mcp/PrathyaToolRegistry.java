package dev.prathya.mcp;

import dev.prathya.mcp.tools.ReadToolHandlers;
import dev.prathya.mcp.tools.ToolSchemas;
import dev.prathya.mcp.tools.WriteToolHandlers;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * Registers all 13 Prathya MCP tools on the server.
 */
public class PrathyaToolRegistry {

    private final ReadToolHandlers read;
    private final WriteToolHandlers write;

    public PrathyaToolRegistry(PrathyaServerConfig config) {
        this.read = new ReadToolHandlers(config);
        this.write = new WriteToolHandlers(config);
    }

    public void registerAll(McpSyncServer server) {
        // Read tools (7)
        register(server, "get_contract",
                "Get the full contract — the single source of truth for this module. Returns module " +
                "metadata and all requirements with their statuses, acceptance criteria, corner cases, " +
                "and changelog. Start here to understand what the module is supposed to do.",
                ToolSchemas.CONTRACT_FILE_ONLY,
                read::getContract);

        register(server, "list_requirements",
                "List requirements, optionally filtered by lifecycle status (DRAFT, APPROVED, " +
                "DEPRECATED, SUPERSEDED). Use this to find active work (APPROVED), pending " +
                "decisions (DRAFT), or trace history (DEPRECATED/SUPERSEDED).",
                ToolSchemas.LIST_REQUIREMENTS,
                read::listRequirements);

        register(server, "get_requirement",
                "Get full details of a single requirement by ID, including its acceptance criteria, " +
                "corner cases, version history, and supersession chain.",
                ToolSchemas.GET_REQUIREMENT,
                read::getRequirement);

        register(server, "list_untested",
                "List requirements that have no @Requirement annotation mapping them to any test " +
                "method. These are gaps in the contract — intent was defined but no test verifies " +
                "it. Requires --test-classes to be configured.",
                ToolSchemas.CONTRACT_FILE_ONLY,
                read::listUntested);

        register(server, "get_coverage_matrix",
                "Get the full coverage matrix — a three-state view of every requirement and corner " +
                "case: covered+passing (contract satisfied), covered+failing (contract broken), " +
                "or not covered (contract unverified). This is the primary quality signal.",
                ToolSchemas.CONTRACT_FILE_ONLY,
                read::getCoverageMatrix);

        register(server, "run_audit",
                "Run the audit engine to detect contract violations: orphaned @Requirement " +
                "annotations referencing IDs not in CONTRACT.yaml, approved requirements with " +
                "no tests, deprecated requirements still being tested, and coverage below " +
                "threshold. Fix violations to keep the contract and code in sync.",
                ToolSchemas.CONTRACT_FILE_ONLY,
                read::runAudit);

        register(server, "validate_contract",
                "Validate CONTRACT.yaml for structural errors: malformed YAML, duplicate IDs, " +
                "invalid status values, broken supersession references, and schema violations. " +
                "Run this after manual edits to catch mistakes before they propagate.",
                ToolSchemas.CONTRACT_FILE_ONLY,
                read::validateContract);

        // Write tools (6)
        register(server, "add_requirement",
                "Add a new requirement to the contract. New requirements start as DRAFT by " +
                "default. An ID is auto-generated following the module's prefix convention if " +
                "not explicitly provided. Include acceptance criteria — these are the checkable " +
                "proofs that tests will verify.",
                ToolSchemas.ADD_REQUIREMENT,
                write::addRequirement);

        register(server, "update_requirement",
                "Update fields of an existing requirement (title, description, acceptance " +
                "criteria, version). Include a changelog note explaining what changed and why. " +
                "For significant behavioral changes, consider supersede_requirement instead — " +
                "it preserves the original and creates a new ID with a back-reference.",
                ToolSchemas.UPDATE_REQUIREMENT,
                write::updateRequirement);

        register(server, "add_corner_case",
                "Add a corner case to a requirement. Corner cases are first-class citizens — " +
                "each gets its own ID (e.g. AUTH-001-CC-001) and is independently tracked in " +
                "coverage. They represent the edge conditions and error paths that often harbor " +
                "bugs. A corner case ID is auto-generated if not provided.",
                ToolSchemas.ADD_CORNER_CASE,
                write::addCornerCase);

        register(server, "update_corner_case",
                "Update the description of an existing corner case. Corner case IDs are " +
                "permanent — if the semantics change fundamentally, add a new corner case " +
                "instead.",
                ToolSchemas.UPDATE_CORNER_CASE,
                write::updateCornerCase);

        register(server, "deprecate_requirement",
                "Deprecate an APPROVED requirement that is no longer relevant. Deprecated " +
                "requirements are excluded from coverage metrics but remain in the contract " +
                "for traceability. Use this when a requirement becomes obsolete without a " +
                "replacement. If a replacement exists, use supersede_requirement instead.",
                ToolSchemas.DEPRECATE_REQUIREMENT,
                write::deprecateRequirement);

        register(server, "supersede_requirement",
                "Replace an existing requirement with a new one. The old requirement's status " +
                "changes to SUPERSEDED with a superseded_by reference, and a new requirement " +
                "is created with a supersedes back-reference. This preserves the full evolution " +
                "history — IDs are never deleted or reused. Use this when a requirement changes " +
                "significantly enough to warrant a new identity.",
                ToolSchemas.SUPERSEDE_REQUIREMENT,
                write::supersedeRequirement);
    }

    @FunctionalInterface
    private interface ToolHandler {
        McpSchema.CallToolResult handle(Map<String, Object> args);
    }

    private void register(McpSyncServer server, String name, String description,
                          String inputSchema, ToolHandler handler) {
        McpJsonMapper mapper = McpJsonDefaults.getMapper();
        var tool = McpSchema.Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(mapper, inputSchema)
                .build();
        var spec = new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, request) -> handler.handle(
                        request.arguments() != null ? request.arguments() : Map.of()
                )
        );
        server.addTool(spec);
    }
}
