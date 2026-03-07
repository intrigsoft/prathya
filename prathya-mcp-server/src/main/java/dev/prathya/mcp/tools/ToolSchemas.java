package dev.prathya.mcp.tools;

/**
 * JSON Schema constants for MCP tool input definitions.
 */
public final class ToolSchemas {

    private ToolSchemas() {}

    public static final String EMPTY = """
            {"type": "object", "properties": {}}
            """;

    public static final String GET_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "id": { "type": "string", "description": "Requirement ID (e.g. ORD-001)" }
              },
              "required": ["id"]
            }
            """;

    public static final String LIST_REQUIREMENTS = """
            {
              "type": "object",
              "properties": {
                "status": { "type": "string", "description": "Filter by status (DRAFT, APPROVED, DEPRECATED, SUPERSEDED)" }
              }
            }
            """;

    public static final String ADD_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "title":    { "type": "string", "description": "Requirement title" },
                "id":       { "type": "string", "description": "Optional explicit ID (auto-generated if omitted)" },
                "description": { "type": "string", "description": "Requirement description" },
                "status":   { "type": "string", "description": "Status (default: DRAFT)" },
                "acceptance_criteria": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "List of acceptance criteria"
                }
              },
              "required": ["title"]
            }
            """;

    public static final String UPDATE_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "id":       { "type": "string", "description": "Requirement ID to update" },
                "title":    { "type": "string", "description": "New title" },
                "description": { "type": "string", "description": "New description" },
                "version":  { "type": "string", "description": "New version" },
                "acceptance_criteria": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "New acceptance criteria"
                },
                "note": { "type": "string", "description": "Changelog note for this update" }
              },
              "required": ["id"]
            }
            """;

    public static final String ADD_CORNER_CASE = """
            {
              "type": "object",
              "properties": {
                "req_id":      { "type": "string", "description": "Parent requirement ID" },
                "description": { "type": "string", "description": "Corner case description" },
                "id":          { "type": "string", "description": "Optional explicit CC ID (auto-generated if omitted)" }
              },
              "required": ["req_id", "description"]
            }
            """;

    public static final String UPDATE_CORNER_CASE = """
            {
              "type": "object",
              "properties": {
                "req_id":      { "type": "string", "description": "Parent requirement ID" },
                "cc_id":       { "type": "string", "description": "Corner case ID to update" },
                "description": { "type": "string", "description": "New description" }
              },
              "required": ["req_id", "cc_id", "description"]
            }
            """;

    public static final String DEPRECATE_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "id":     { "type": "string", "description": "Requirement ID to deprecate" },
                "reason": { "type": "string", "description": "Reason for deprecation" }
              },
              "required": ["id"]
            }
            """;

    public static final String SUPERSEDE_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "old_id":   { "type": "string", "description": "Requirement ID to supersede" },
                "title":    { "type": "string", "description": "Title for the new replacement requirement" },
                "new_id":   { "type": "string", "description": "Optional explicit ID for replacement (auto-generated if omitted)" },
                "description": { "type": "string", "description": "Description for replacement" },
                "acceptance_criteria": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Acceptance criteria for replacement"
                }
              },
              "required": ["old_id", "title"]
            }
            """;
}
