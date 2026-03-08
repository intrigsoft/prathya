# <img src="site/docs/assets/logo.svg" alt="Prathya" height="32"> Prathya

[![CI](https://github.com/intrigsoft/prathya/actions/workflows/ci.yml/badge.svg)](https://github.com/intrigsoft/prathya/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Contract-Driven Requirement Coverage for Java**

Prathya is an open-source Java tool that brings formal requirement traceability to software testing. It treats requirements as first-class, versioned artifacts, links tests to those requirements via annotations, and measures **requirement coverage** — a more meaningful quality signal than code coverage alone.

## The Core Insight

Code coverage tells you what was _touched_. Requirement coverage tells you whether _intent_ was verified.

Prathya introduces **Contract-Driven Development (CDD)** as a natural companion to established methodologies:

| Methodology | Focus | Gap |
|---|---|---|
| **TDD** | Write the test first, then the code | Drives implementation but says nothing about whether the tests are the *right* tests |
| **BDD** | Write behavior specifications in natural language | Improves communication but doesn't enforce traceability or coverage measurement |
| **CDD** | Define the contract first. Tests are written against the contract | The contract is the source of truth — not the code, not the tests, not a ticket |

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>dev.prathya</groupId>
    <artifactId>prathya-annotations</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### 2. Create `CONTRACT.yaml`

```yaml
module:
  id: AUTH
  name: Auth Service
  description: Handles user authentication

requirements:
  - id: AUTH-001
    version: 1.0.0
    status: approved
    title: User login with valid credentials
    description: >
      The system must authenticate a user given valid credentials,
      returning a signed JWT.
    acceptance_criteria:
      - Returns HTTP 200 with a JWT in the response body
    corner_cases:
      - id: AUTH-001-CC-001
        description: Invalid password — must return 401
```

### 3. Annotate your tests

```java
import dev.prathya.annotations.Requirement;

class AuthServiceTest {

    @Test
    @Requirement("AUTH-001")
    void login_validCredentials_returnsJwt() {
        // ...
    }

    @Test
    @Requirement("AUTH-001-CC-001")
    void login_wrongPassword_returns401() {
        // ...
    }
}
```

### 4. Run

```bash
mvn clean verify
```

Prathya runs after tests complete and produces an HTML report at `target/prathya/index.html` and a JSON report at `target/prathya/prathya-report.json`.

## Gradle

```kotlin
plugins {
    id("dev.prathya.gradle") version "1.0.0-SNAPSHOT"
}

dependencies {
    testImplementation("dev.prathya:prathya-annotations:1.0.0-SNAPSHOT")
}
```

```bash
./gradlew prathyaVerify
```

See the [Gradle Plugin docs](https://intrigsoft.github.io/prathya/gradle-plugin/) for configuration and available tasks.

## MCP Server

Prathya includes an [MCP server](https://intrigsoft.github.io/prathya/mcp-server/) that lets AI coding agents participate directly in the contract-driven development loop — reading the contract, checking coverage, and iterating until it's satisfied.

Add it to your MCP configuration with [JBang](https://www.jbang.dev/):

```json
{
  "mcpServers": {
    "prathya": {
      "command": "jbang",
      "args": [
        "--quiet",
        "--main", "dev.prathya.mcp.PrathyaMcpServer",
        "dev.prathya:prathya-mcp-server:1.0.0-SNAPSHOT"
      ]
    }
  }
}
```

## Documentation

Full documentation at [intrigsoft.github.io/prathya](https://intrigsoft.github.io/prathya):

- [Concepts](https://intrigsoft.github.io/prathya/concepts/) — understand Contract-Driven Development in depth
- [CONTRACT.yaml Reference](https://intrigsoft.github.io/prathya/contract-format/) — full schema and field reference
- [Maven Plugin](https://intrigsoft.github.io/prathya/maven-plugin/) — setup, goals, and configuration
- [Gradle Plugin](https://intrigsoft.github.io/prathya/gradle-plugin/) — Gradle integration
- [MCP Server](https://intrigsoft.github.io/prathya/mcp-server/) — AI agent integration via Model Context Protocol
- [Audit Rules](https://intrigsoft.github.io/prathya/audit-rules/) — what Prathya checks and how to configure it

## License

[Apache License 2.0](LICENSE)
