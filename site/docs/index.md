# Prathya

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
    <groupId>com.intrigsoft.prathya</groupId>
    <artifactId>prathya-annotations</artifactId>
    <version>0.6.2-SNAPSHOT</version>
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
import com.intrigsoft.prathya.annotations.Requirement;

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

## What's Next

- [Concepts](concepts.md) — understand Contract-Driven Development in depth
- [CONTRACT.yaml Reference](contract-format.md) — full schema and field reference
- [Maven Plugin](maven-plugin.md) — setup, goals, and configuration
- [Gradle Plugin](gradle-plugin.md) — Gradle integration
- [MCP Server](mcp-server.md) — AI agent integration via Model Context Protocol
- [Audit Rules](audit-rules.md) — what Prathya checks and how to configure it
- [Roadmap](roadmap.md) — version history and future plans
