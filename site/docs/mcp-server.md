# MCP Server

The Prathya MCP server exposes the contract management surface via the [Model Context Protocol](https://modelcontextprotocol.io/), enabling AI coding agents to participate directly in the contract-driven development loop.

An agent can read the contract before generating code, check coverage after generating tests, and iterate until the contract is satisfied.

## Prerequisites

### 1. Install JBang

[JBang](https://www.jbang.dev/) is used to run the MCP server without manual classpath setup.

=== "Linux / macOS"

    ```bash
    curl -Ls https://sh.jbang.dev | bash -s - app setup
    ```

=== "Windows"

    ```powershell
    iex "& { $(iwr -useb https://ps.jbang.dev) } app setup"
    ```

=== "SDKMAN!"

    ```bash
    sdk install jbang
    ```

### 2. Install the MCP Server Artifact

Fetch the Prathya MCP server uber-jar into your local Maven repository:

```bash
mvn dependency:get -Dartifact=com.intrigsoft.prathya:prathya-mcp-server:0.6.1
```

## Configuration

Add the Prathya MCP server to your AI coding tool's MCP configuration. The server uses **stdio** transport.

### Generic MCP Configuration

```json
{
  "mcpServers": {
    "prathya": {
      "command": "jbang",
      "args": [
        "--quiet",
        "--main", "com.intrigsoft.prathya.mcp.PrathyaMcpServer",
        "com.intrigsoft.prathya:prathya-mcp-server:0.6.1"
      ]
    }
  }
}
```

### Configuration Options

| Option | Default | Description |
|---|---|---|
| `--contract-file` | `CONTRACT.yaml` | Path to the contract file |
| `--test-classes` | `target/test-classes` | Path to compiled test classes |

To pass options, append them after the artifact coordinate:

```json
{
  "mcpServers": {
    "prathya": {
      "command": "jbang",
      "args": [
        "--quiet",
        "--main", "com.intrigsoft.prathya.mcp.PrathyaMcpServer",
        "com.intrigsoft.prathya:prathya-mcp-server:0.6.1",
        "--contract-file", "/path/to/CONTRACT.yaml",
        "--test-classes", "/path/to/target/test-classes"
      ]
    }
  }
}
```

## Tool Reference

The server exposes 13 tools organized into read and write operations.

### Read Tools

| Tool | Description |
|---|---|
| `get_contract` | Returns the full parsed contract as JSON |
| `list_requirements` | Lists all requirements with ID, title, status, and version |
| `get_requirement` | Returns full details for a specific requirement by ID |
| `list_untested` | Lists approved requirements and corner cases with no mapped tests |
| `get_coverage_matrix` | Returns the full coverage matrix: requirements, mapped tests, and coverage percentages |
| `run_audit` | Runs the audit engine and returns all violations |
| `validate_contract` | Validates the CONTRACT.yaml against the schema |

### Write Tools

| Tool | Description |
|---|---|
| `add_requirement` | Adds a new requirement to the contract |
| `update_requirement` | Updates an existing requirement's fields |
| `add_corner_case` | Adds a corner case to an existing requirement |
| `update_corner_case` | Updates an existing corner case |
| `deprecate_requirement` | Sets a requirement's status to `deprecated` |
| `supersede_requirement` | Marks a requirement as superseded and links to its replacement |

## Agent Workflow

A typical workflow for an AI agent using the MCP server:

1. **Read the contract** — `get_contract` or `list_requirements` to understand what the module must do
2. **Check coverage** — `list_untested` to find gaps
3. **Generate tests** — write tests annotated with `@Requirement` for uncovered items
4. **Verify** — `get_coverage_matrix` to confirm coverage improved
5. **Iterate** — repeat until the contract is fully satisfied

## Technical Details

- **Transport:** stdio (standard input/output)
- **Packaging:** Maven Shade uber-jar
- **SDK:** MCP SDK `io.modelcontextprotocol.sdk:mcp:1.0.0`
