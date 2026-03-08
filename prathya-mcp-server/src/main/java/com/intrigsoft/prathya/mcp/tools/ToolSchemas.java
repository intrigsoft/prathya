package com.intrigsoft.prathya.mcp.tools;

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
                "id": { "type": "string", "description": "Requirement ID (e.g. AUTH-001) or corner case ID (e.g. AUTH-001-CC-001). IDs are permanent and immutable — they never change or get reused." },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["id"]
            }
            """;

    public static final String LIST_REQUIREMENTS = """
            {
              "type": "object",
              "properties": {
                "status": { "type": "string", "description": "Filter by lifecycle status: DRAFT (under discussion, not yet active), APPROVED (active contract, should be tested), DEPRECATED (no longer relevant, kept for history), or SUPERSEDED (replaced by a newer requirement)." },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              }
            }
            """;

    public static final String ADD_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "title":    { "type": "string", "description": "Requirement title — a concise statement of what the system must do." },
                "id":       { "type": "string", "description": "Explicit ID following the {MODULE}-{SEQUENCE} convention (e.g. AUTH-004). Auto-generated from the module prefix if omitted. Once assigned, IDs are permanent and never reused." },
                "description": { "type": "string", "description": "Detailed description of the requirement — the full behavioral specification." },
                "status":   { "type": "string", "description": "Lifecycle status. Defaults to DRAFT (under discussion). Set to APPROVED when the requirement is part of the active contract and should be tested." },
                "acceptance_criteria": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Checkable proofs that tests will verify. Each criterion should be a concrete, testable statement that can unambiguously pass or fail."
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
                "id":       { "type": "string", "description": "Requirement ID to update. The ID itself never changes — use supersede_requirement if the change is significant enough to warrant a new identity." },
                "title":    { "type": "string", "description": "New title." },
                "description": { "type": "string", "description": "New description." },
                "version":  { "type": "string", "description": "New semver version. Major = breaking change (tests must be re-evaluated), Minor = additive change (new corner case, expanded scope), Patch = wording/clarification only." },
                "acceptance_criteria": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Replacement acceptance criteria. Each criterion should be a concrete, testable statement."
                },
                "note": { "type": "string", "description": "Changelog entry explaining what changed and why. Appended to the requirement's immutable, append-only changelog history." },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["id"]
            }
            """;

    public static final String ADD_CORNER_CASE = """
            {
              "type": "object",
              "properties": {
                "req_id":      { "type": "string", "description": "Parent requirement ID (e.g. AUTH-001). The corner case will be added under this requirement." },
                "description": { "type": "string", "description": "Corner case description — the edge condition, error path, or boundary behavior to verify. Corner cases are first-class citizens with independent coverage tracking." },
                "id":          { "type": "string", "description": "Explicit corner case ID following the {REQ_ID}-CC-{N} convention (e.g. AUTH-001-CC-003). Auto-generated if omitted. Once assigned, IDs are permanent." },
                "test_environment": { "type": "string", "enum": ["unit", "integration", "full-server"], "description": "Required test environment for this corner case. When set, uncovered corner cases produce an INFO-level notice instead of a WARN." },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["req_id", "description"]
            }
            """;

    public static final String UPDATE_CORNER_CASE = """
            {
              "type": "object",
              "properties": {
                "req_id":      { "type": "string", "description": "Parent requirement ID (e.g. AUTH-001)." },
                "cc_id":       { "type": "string", "description": "Corner case ID to update (e.g. AUTH-001-CC-001). The ID itself never changes." },
                "description": { "type": "string", "description": "New description for the corner case." },
                "test_environment": { "type": "string", "enum": ["unit", "integration", "full-server"], "description": "Required test environment for this corner case." },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["req_id", "cc_id"]
            }
            """;

    public static final String DEPRECATE_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "id":     { "type": "string", "description": "Requirement ID to deprecate. Only APPROVED requirements can be deprecated. The requirement remains in the contract for traceability but is excluded from coverage metrics." },
                "reason": { "type": "string", "description": "Reason for deprecation — why this requirement is no longer relevant. Recorded in the changelog." },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["id"]
            }
            """;

    public static final String SUPERSEDE_REQUIREMENT = """
            {
              "type": "object",
              "properties": {
                "old_id":   { "type": "string", "description": "Requirement ID to supersede. Its status will change to SUPERSEDED with a superseded_by reference to the new requirement. The old ID is preserved — never deleted." },
                "title":    { "type": "string", "description": "Title for the new replacement requirement." },
                "new_id":   { "type": "string", "description": "Explicit ID for the replacement requirement. Auto-generated if omitted. Creates a supersedes back-reference to old_id, forming a traceable evolution chain." },
                "description": { "type": "string", "description": "Description for the replacement requirement." },
                "acceptance_criteria": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Acceptance criteria for the replacement requirement."
                },
                "contract_file": { "type": "string", "description": "Path to CONTRACT.yaml file. Defaults to CONTRACT.yaml in working directory if omitted." }
              },
              "required": ["old_id", "title"]
            }
            """;
}
