package dev.prathya.mcp.tools;

/**
 * JSON Schema constants for MCP tool input definitions.
 */
public final class ToolSchemas {

    private ToolSchemas() {}

    public static final String EMPTY = """
            {"type": "object", "properties": {}}
            """;

    /**
     * Schema with only the optional contract_file property.
     * Used for tools that previously had an empty schema.
     */
    public static final String CONTRACT_FILE_ONLY = """
            {
              "type": "object",
              "properties": {
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              }
            }
            """;

    public static final String GET_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "id": { "type": "string", "description": "Requirement ID (e.g. ORD-001)" },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["id"]
            }
            """;

    public static final String LIST_REQUIREMENTS = """
            {
              "type": "object",
              "properties": {
                "status": { "type": "string", "description": "Filter by status (DRAFT, APPROVED, DEPRECATED, SUPERSEDED)" },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
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
                },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
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
                "note": { "type": "string", "description": "Changelog note for this update" },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
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
                "id":          { "type": "string", "description": "Optional explicit CC ID (auto-generated if omitted)" },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
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
                "description": { "type": "string", "description": "New description" },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["req_id", "cc_id", "description"]
            }
            """;

    public static final String DEPRECATE_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "id":     { "type": "string", "description": "Requirement ID to deprecate" },
                "reason": { "type": "string", "description": "Reason for deprecation" },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
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
                },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["old_id", "title"]
            }
            """;
}
