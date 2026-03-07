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
                "Get the full contract summary (module info + all requirements)",
                ToolSchemas.EMPTY,
                read::getContract);

        register(server, "list_requirements",
                "List all requirements, optionally filtered by status",
                ToolSchemas.LIST_REQUIREMENTS,
                read::listRequirements);

        register(server, "get_requirement",
                "Get full details of a single requirement by ID",
                ToolSchemas.GET_REQUIREMENT,
                read::getRequirement);

        register(server, "list_untested",
                "List requirements that have no test coverage (requires --test-classes)",
                ToolSchemas.EMPTY,
                read::listUntested);

        register(server, "get_coverage_matrix",
                "Get the full coverage matrix showing which requirements and corner cases are tested",
                ToolSchemas.EMPTY,
                read::getCoverageMatrix);

        register(server, "run_audit",
                "Run the audit engine to find violations (orphaned annotations, uncovered requirements, etc.)",
                ToolSchemas.EMPTY,
                read::runAudit);

        register(server, "validate_contract",
                "Validate the CONTRACT.yaml for format errors, duplicate IDs, and consistency",
                ToolSchemas.EMPTY,
                read::validateContract);

        // Write tools (6)
        register(server, "add_requirement",
                "Add a new requirement to the contract",
                ToolSchemas.ADD_REQUIREMENT,
                write::addRequirement);

        register(server, "update_requirement",
                "Update fields of an existing requirement",
                ToolSchemas.UPDATE_REQUIREMENT,
                write::updateRequirement);

        register(server, "add_corner_case",
                "Add a corner case to an existing requirement",
                ToolSchemas.ADD_CORNER_CASE,
                write::addCornerCase);

        register(server, "update_corner_case",
                "Update the description of an existing corner case",
                ToolSchemas.UPDATE_CORNER_CASE,
                write::updateCornerCase);

        register(server, "deprecate_requirement",
                "Deprecate an APPROVED requirement",
                ToolSchemas.DEPRECATE_REQUIREMENT,
                write::deprecateRequirement);

        register(server, "supersede_requirement",
                "Supersede an old requirement with a new replacement",
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
