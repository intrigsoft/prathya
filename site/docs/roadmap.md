# Roadmap

## Version Plan

| Version | Milestone | Description |
|---|---|---|
| **v0.1** | Core | `prathya-annotations` + `prathya-core` — parser, scanner, coverage computation |
| **v0.2** | Maven Plugin | `prathya-maven-plugin` — `verify` goal, HTML + JSON report via JMustache |
| **v0.3** | CI Gate | Fail build on violations, minimum coverage threshold |
| **v0.4** | Test Runner | `prathya:run` focused test runner — Surefire-based, three-state coverage |
| **v0.5** | Failsafe | Failsafe support in `prathya:run` for integration test execution |
| **v0.6** | Aggregation | Multi-module aggregation (`prathya:aggregate`) |
| **v0.7** | Gradle | `prathya-gradle-plugin` |
| **v0.8** | IntelliJ | `prathya-intellij-plugin` — requirement authoring UI, traceability gutter icons, validation |
| **v1.0** | GA | Maven Central publication, stable schema, production-ready |

## Module Structure

```
prathya/
├── prathya-annotations/       # @Requirement annotation — minimal JAR, zero deps
├── prathya-core/              # Domain model, YAML parser, coverage, reporting
├── prathya-maven-plugin/      # Maven lifecycle integration
├── prathya-gradle-plugin/     # Gradle task integration
├── prathya-mcp-server/        # MCP server for AI agent integration
└── prathya-intellij-plugin/   # IntelliJ IDEA plugin (planned)
```

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven (parent POM) |
| YAML parsing | SnakeYAML |
| JSON output | Jackson Databind |
| HTML report | JMustache (embedded classpath templates) |
| Annotation scanning | Java Reflection API |
| Test execution | Maven Invoker API (delegates to Surefire) |
| MCP SDK | `io.modelcontextprotocol.sdk:mcp:1.0.0` |
| Distribution | Maven Central |
| License | Apache 2.0 |

## Future Considerations (Post v1.0)

### Test Generation

Given a `CONTRACT.yaml`, generate test method stubs pre-annotated with `@Requirement` IDs — one per requirement and corner case. Reduces the bootstrap cost of writing contract tests and ensures no requirement is missed. A natural fit for AI-assisted generation.

### MCP Server Enhancements

Expand the MCP server with additional tools for more advanced agent workflows — test generation triggers, coverage trend analysis, and cross-module dependency tracking.

### IntelliJ Plugin

A quality-of-life layer featuring:

- **Three-mode view** — Source (raw YAML), Rendered (structured cards), and Split (live sync)
- **Schema-driven autocompletion** — via JSON Schema published to SchemaStore.org
- **Traceability gutter icons** — navigate between `@Requirement` annotations and CONTRACT.yaml
- **Inline audit warnings** — unknown IDs, deprecated references, uncovered requirements
- **`prathya:run` integration** — run contract tests for a specific requirement from the IDE
