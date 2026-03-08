# Prathya — Contract-Driven Requirement Coverage for Java

## Overview

Prathya is an open-source Java tool that brings formal requirement traceability to software testing. It treats requirements as first-class, versioned artifacts, links tests to those requirements via annotations, and measures **requirement coverage** — a more meaningful quality signal than code coverage alone.

Prathya introduces **Contract-Driven Development (CDD)** as a natural companion to established methodologies:

- **TDD** — write the test first, then the code. Drives implementation but says nothing about whether the tests are the *right* tests.
- **BDD** — write behavior specifications in natural language. Improves communication but doesn't enforce traceability, versioning, or coverage measurement against a formal contract.
- **CDD** — define the contract first. Tests are written against the contract. Coverage is measured against the contract. The contract is the source of truth — not the code, not the tests, not a ticket in a project management tool.

The core insight: code coverage tells you what was _touched_. Requirement coverage tells you whether _intent_ was verified.

Prathya is designed to sit alongside JaCoCo in the Java build lifecycle. Used together, the two metrics expose a quadrant of insights:

| | Code Coverage High | Code Coverage Low |
|---|---|---|
| **Requirement Coverage High** | Healthy | Dead code or over-abstraction |
| **Requirement Coverage Low** | Undocumented/missing features | Chaos — prototype territory |

---

## Core Concepts

### The Contract (`CONTRACT.yaml`)
Every module has a `CONTRACT.yaml` file that defines its behavioral contract. This is a human-authored, version-controlled artifact that defines what the module is supposed to do — including corner cases as first-class citizens, not afterthoughts.

### The Annotation (`@Requirement`)
Test methods are annotated with `@Requirement("REQ-ID")` to declare what requirement or corner case they verify. This is the only coupling between test code and requirements.

### Requirement Coverage
Prathya scans annotations at build time, cross-references them against `CONTRACT.yaml`, and computes a coverage matrix. It produces an HTML report and a machine-readable JSON for CI integration.

### No Manual Trace File
The trace is derived from annotations at build time — no manually maintained mapping file is needed. The annotations _are_ the trace. This keeps the maintenance burden minimal.

---

## Requirement ID Conventions

IDs are **permanent and immutable**. They are opaque labels — meaning lives in the content, not the number.

### Format
```
{MODULE}-{SEQUENCE}         → AUTH-001       (requirement)
{MODULE}-{SEQUENCE}-CC-{N}  → AUTH-001-CC-002 (corner case)
```

### Rules
- IDs are **append-only and never reused**
- Requirements are **never deleted** — only `deprecated` or `superseded`
- When a requirement changes significantly it gets a **new ID** with a `supersedes` back-reference
- When a requirement changes in wording/clarification only, the **version increments** on the same ID
- When a requirement is split, the original is deprecated and two new IDs created with `supersedes`

### Versioning
Requirement versions follow semver semantics:
- **Major** — breaking change to the contract, mapped tests must be re-evaluated
- **Minor** — additive change (new corner case, expanded scope)
- **Patch** — wording or clarification, no behavioral change

---

## File Formats

### CONTRACT.yaml

```yaml
module:
  id: AUTH
  name: Authentication Module
  description: Handles user authentication, token issuance, and session management
  owner: team@example.com
  created: 2026-03-06
  version: 1.2.0

requirements:

  - id: AUTH-001
    version: 1.1.0
    status: approved           # draft | approved | deprecated | superseded
    title: User login with email and password
    description: >
      The system must authenticate a user given a valid email and password combination,
      issuing a signed JWT access token and a refresh token upon success.
    acceptance_criteria:
      - Valid credentials return a signed JWT access token and refresh token
      - Access token expiry is set to 15 minutes
      - Refresh token expiry is set to 7 days
    corner_cases:
      - id: AUTH-001-CC-001
        description: Email is valid but password is incorrect — must return 401, not 404
      - id: AUTH-001-CC-002
        description: Email does not exist — response must be identical to wrong password to prevent enumeration
      - id: AUTH-001-CC-003
        description: Email is provided in mixed case — must be normalized before lookup
    changelog:
      - version: 1.0.0
        date: 2026-01-10
        note: Initial definition
      - version: 1.1.0
        date: 2026-02-18
        note: Added enumeration prevention requirement to CC-002

  - id: AUTH-002
    version: 1.0.0
    status: approved
    title: JWT refresh
    description: >
      The system must issue a new access token when presented with a valid, non-expired refresh token.
    acceptance_criteria:
      - Valid refresh token returns a new access token
      - Original refresh token is rotated (invalidated and replaced)
      - Expired refresh token returns 401
    corner_cases:
      - id: AUTH-002-CC-001
        description: Refresh token is used twice — second use must fail (rotation enforcement)
      - id: AUTH-002-CC-002
        description: Refresh token from a revoked session must be rejected even if not expired
    changelog:
      - version: 1.0.0
        date: 2026-01-10
        note: Initial definition

  - id: AUTH-003
    version: 1.0.0
    status: superseded
    superseded_by: AUTH-005
    title: Single-factor password reset (superseded)
    description: >
      The system must allow password reset via email link only.
    acceptance_criteria:
      - Reset link is valid for 30 minutes
    corner_cases: []
    changelog:
      - version: 1.0.0
        date: 2026-01-10
        note: Initial definition
      - version: 1.0.0
        date: 2026-02-01
        note: Superseded by AUTH-005 (MFA-aware reset)

  - id: AUTH-005
    version: 1.0.0
    status: approved
    supersedes: AUTH-003
    title: MFA-aware password reset
    description: >
      The system must allow password reset via email link, with optional MFA verification
      step before the reset is permitted.
    acceptance_criteria:
      - Reset link is valid for 15 minutes
      - If MFA is enabled on the account, OTP verification is required before reset
      - Reset invalidates all existing sessions
    corner_cases:
      - id: AUTH-005-CC-001
        description: Reset link is used after expiry — must return 410 Gone, not 401
      - id: AUTH-005-CC-002
        description: User attempts reset while already logged in — session must still be invalidated
    changelog:
      - version: 1.0.0
        date: 2026-02-01
        note: Supersedes AUTH-003. Adds MFA step and session invalidation.
```

---

## Annotation Usage

```java
import com.intrigsoft.prathya.annotations.Requirement;

class AuthServiceTest {

    @Test
    @Requirement("AUTH-001")
    void loginWithValidCredentials_returnsTokens() {
        // ...
    }

    @Test
    @Requirement({"AUTH-001-CC-001", "AUTH-001-CC-002"})
    void loginWithUnknownEmail_returnsIdenticalResponseToWrongPassword() {
        // ...
    }

    @Test
    @Requirement("AUTH-002-CC-001")
    void refreshTokenUsedTwice_secondCallFails() {
        // ...
    }
}
```

One test can cover multiple requirements or corner cases. One requirement can be covered by multiple tests. The relationship is many-to-many.

---

## Project Structure

```
prathya/
├── prathya-annotations/          # @Requirement annotation only — minimal JAR, no transitive deps
├── prathya-core/                 # Domain model, YAML parser, coverage computation, reporting
├── prathya-maven-plugin/         # Maven lifecycle integration
├── prathya-gradle-plugin/        # Gradle task integration (second target)
├── prathya-intellij-plugin/      # IntelliJ IDEA plugin — authoring UI, gutter icons, validation
└── prathya-examples/             # Example Java project demonstrating usage
```

### Parent POM
All modules share a parent `pom.xml` for version management. Published to Maven Central as a group.

---

## Module Details

### `prathya-annotations`

A minimal JAR containing only the `@Requirement` annotation. Kept separate so test code has a lightweight dependency.

```java
package com.intrigsoft.prathya.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Requirement.List.class)
public @interface Requirement {
    String[] value();              // One or more requirement/corner case IDs

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        Requirement[] value();
    }
}
```

**Dependencies:** None. Java only.

---

### `prathya-core`

The framework-agnostic engine. Contains:

- **YAML parser** — reads and validates `CONTRACT.yaml` against schema
- **Domain model** — `Requirement`, `CornerCase`, `RequirementStatus`, `TraceEntry`, `CoverageMatrix`
- **Reflection scanner** — given a list of compiled test class files, scans for `@Requirement` annotations and builds trace entries
- **Coverage computer** — cross-references trace against requirements, computes coverage percentages per requirement, per module, and overall
- **Report generator** — produces HTML report and JSON summary
- **Audit engine** — flags orphaned annotations (ID in `@Requirement` not found in CONTRACT.yaml), untested requirements, deprecated requirements still being tested
- **Test runner bridge** — given a filtered set of test class/method names, constructs the Surefire filter string and invokes `mvn test` via Maven Invoker API; parses resulting `TEST-*.xml` reports and maps pass/fail back to requirement IDs

**Dependencies:**
- `org.yaml:snakeyaml` — YAML parsing
- `com.fasterxml.jackson.core:jackson-databind` — JSON report output
- `com.samskivert:jmustache` — HTML report templating

**Key interfaces:**

```java
public interface RequirementParser {
    ModuleContract parse(Path requirementYaml) throws PrathyaException;
}

public interface AnnotationScanner {
    List<TraceEntry> scan(List<Path> testClassDirectories);
}

public interface CoverageReporter {
    CoverageMatrix compute(ModuleContract contract, List<TraceEntry> traces);
    void writeHtmlReport(CoverageMatrix matrix, Path outputDir);
    void writeJsonReport(CoverageMatrix matrix, Path outputFile);
}

public interface PrathyaTestRunner {
    // Resolves which test methods are mapped to active requirements in the contract
    List<TestMethod> resolveTests(ModuleContract contract, List<TraceEntry> traces);
    // Parses standard Surefire XML reports and maps results back to requirements
    TestRunResult parseResults(Path surefireReportsDir, List<TestMethod> expectedTests);
}
```

---

### `prathya-maven-plugin`

Hooks into the Maven `verify` phase (after `test` phase completes).

**Plugin coordinates:**
```xml
<groupId>com.intrigsoft.prathya</groupId>
<artifactId>prathya-maven-plugin</artifactId>
```

**Usage in project `pom.xml`:**
```xml
<plugin>
    <groupId>com.intrigsoft.prathya</groupId>
    <artifactId>prathya-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <requirementFile>${project.basedir}/CONTRACT.yaml</requirementFile>
        <testClassesDirectory>${project.build.testOutputDirectory}</testClassesDirectory>
        <outputDirectory>${project.build.directory}/prathya</outputDirectory>
        <failOnViolations>true</failOnViolations>
        <minimumRequirementCoverage>80</minimumRequirementCoverage>  <!-- percentage -->
        <excludeStatuses>
            <status>deprecated</status>
            <status>superseded</status>
        </excludeStatuses>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Plugin goals:**
- `prathya:verify` — full scan, coverage computation, report generation, optional build failure
- `prathya:run` — resolves tests mapped to active requirements, executes them via Surefire, computes coverage from results
- `prathya:report` — report only, no build failure gate
- `prathya:audit` — prints violations to console only

**Maven lifecycle binding:** `verify` phase (after `test`).

**Implementation approach:** Reflection (not bytecode scanning). The plugin constructs a classloader over `testClassesDirectory` and loads test classes to read `@Requirement` annotations. This works reliably in Maven's `verify` phase since test classes are fully compiled by then.

---

## Focused Test Runner (`prathya:run`)

The `prathya:run` goal shifts Prathya from **passive measurement** to **active execution**. It is a standalone orchestrator — it does not implement a test runner itself. Instead it delegates entirely to Surefire as a black box, using its existing inputs and outputs.

### Surefire-only initially

For simplicity and reliability in the initial implementation, `prathya:run` uses Surefire exclusively — all tests annotated with `@Requirement` are executed via `mvn test`, regardless of whether they are unit or integration tests. This removes the need to classify tests by type, manage multiple runners, or deal with Failsafe lifecycle semantics.

The practical implication: teams place `@Requirement`-annotated tests in standard `*Test.java` classes picked up by Surefire. The convention is simple and universal. Failsafe support is a later milestone once the core flow is proven.

### Execution flow

1. Read `CONTRACT.yaml` from the project root
2. Scan `@Requirement` annotations across compiled test classes
3. Resolve the subset of test methods mapped to **approved** requirements
4. Construct a Surefire-compatible filter string (`ClassName#method+ClassName#method`)
5. Invoke `mvn test -Dtest=<filter>` via **Maven Invoker API** — no forking, no reimplementing
6. Wait for completion
7. Parse `target/surefire-reports/TEST-*.xml` (standard JUnit XML — stable, well-known format)
8. Map results back to requirement IDs via the annotation trace
9. Compute three-state coverage and generate report

Prathya never touches test execution itself. Surefire runs tests exactly as it always does — Prathya just tells it which ones to run and reads the output.

### Three-state coverage model

The report distinguishes between:
- Requirement covered and tests **passing** ✓  — contract satisfied
- Requirement covered but tests **failing** ✗  — contract broken
- Requirement **not covered** by any test — contract unverified

This is more informative than a simple covered/uncovered binary. A covered+failing state is actively worse than not covered — it means the contract was written, a test was written, but the implementation is wrong.

### Standalone usage

Because `prathya:run` uses Maven Invoker, it can be invoked standalone from the command line without being bound to any lifecycle phase.

```bash
# Run all contract tests for approved requirements
mvn prathya:run

# Run tests for a specific requirement only
mvn prathya:run -Dprathya.requirementId=AUTH-001

# Run tests for a specific module
mvn prathya:run -Dprathya.moduleId=AUTH
```

### Filtering options

```xml
<configuration>
    <!-- Run tests for a specific requirement only -->
    <requirementId>AUTH-001</requirementId>

    <!-- Run tests for all approved requirements (default) -->
    <statusFilter>approved</statusFilter>

    <!-- Fail the prathya:run goal if any mapped test fails -->
    <failOnTestFailure>true</failOnTestFailure>

    <!-- Directory to read Surefire XML reports from -->
    <surefireReportsDirectory>${project.build.directory}/surefire-reports</surefireReportsDirectory>
</configuration>
```

### Key implementation note

The `PrathyaTestRunner` interface in `prathya-core` is intentionally decoupled from Maven Invoker. Core only defines the contract — the Maven plugin provides the Invoker-based implementation. This keeps `prathya-core` framework-agnostic and leaves the door open for Failsafe support later without a redesign.

---

## Coverage Report

### HTML Report
JaCoCo-inspired layout, rendered via JMustache templates embedded as classpath resources. Shows:

- **Module summary** — total requirements, covered, uncovered, coverage %
- **Requirement matrix** — per-requirement rows with status, test count, corner case coverage, and test pass/fail state
- **Three-state coverage** — covered+passing (green), covered+failing (red), not covered (grey)
- **Drill-down** — click a requirement to see mapped test methods and which corner cases are covered vs missing
- **Supersession chain** — deprecated/superseded requirements shown with links to successors

### JSON Report (`prathya-report.json`)
Machine-readable. Intended for CI dashboards and quality gates.

```json
{
  "module": "AUTH",
  "generatedAt": "2026-03-06T10:00:00Z",
  "summary": {
    "totalRequirements": 5,
    "activeRequirements": 3,
    "coveredRequirements": 3,
    "requirementCoverage": 100.0,
    "totalCornerCases": 7,
    "coveredCornerCases": 6,
    "cornerCaseCoverage": 85.7
  },
  "requirements": [
    {
      "id": "AUTH-001",
      "status": "approved",
      "covered": true,
      "tests": ["loginWithValidCredentials_returnsTokens"],
      "cornerCases": [
        { "id": "AUTH-001-CC-001", "covered": true, "passing": true },
        { "id": "AUTH-001-CC-002", "covered": true, "passing": true },
        { "id": "AUTH-001-CC-003", "covered": false, "passing": null }
      ]
    }
  ],
  "violations": [
    {
      "type": "UNCOVERED_CORNER_CASE",
      "requirementId": "AUTH-001",
      "cornerCaseId": "AUTH-001-CC-003",
      "message": "Corner case AUTH-001-CC-003 has no mapped test"
    }
  ]
}
```

---

## CI Integration

The plugin fails the build if `failOnViolations` is true and any of the following are detected:

- Requirement coverage below `minimumRequirementCoverage` threshold
- `@Requirement` annotation references an ID not found in `CONTRACT.yaml`
- An `approved` requirement has zero test coverage
- An `approved` corner case has zero test coverage (configurable severity)

This mirrors JaCoCo's minimum coverage gate pattern, which teams already understand.

---

## Multi-Module Projects

For multi-module Maven projects, `CONTRACT.yaml` lives at the module level. Each module has its own contract. The parent build aggregates reports from all child modules into a top-level coverage summary.

Aggregation is handled by a separate `prathya:aggregate` goal bound to the parent POM's `verify` phase.

---

## Audit Rules

The `prathya:audit` goal checks for and reports:

| Rule | Severity |
|---|---|
| `@Requirement` ID not found in CONTRACT.yaml | ERROR |
| Approved requirement with no tests | ERROR |
| Approved requirement with uncovered corner cases | WARN |
| Deprecated requirement still referenced in `@Requirement` | WARN |
| Superseded requirement still referenced in `@Requirement` | WARN |
| Requirement coverage below threshold | ERROR (if threshold configured) |

---

---

## `prathya-intellij-plugin`

A quality-of-life layer on top of the core tool. The underlying format stays YAML — the plugin makes authoring and reviewing it human-friendly without abstracting away the file. Built after the schema stabilises (post v0.6) so the plugin doesn't have to track schema churn.

### Three-mode CONTRACT.yaml view

Mirrors the markdown editing experience developers already know:

- **Source** — raw YAML, full editor. For power users and version control review. No magic, just the file.
- **Rendered** — requirements displayed as structured cards. Status badges, version, acceptance criteria as a checklist, corner cases expandable, supersession chain shown as visible links between cards. Coverage state from last `prathya:run` shown as a colour indicator on each card. Read-only — the primary way to review the contract quickly.
- **Split** — YAML on the left, rendered view on the right, live sync. Edit the YAML and the card updates in real time. Validation errors surface inline. This is the primary authoring experience — you see the structure you're producing as you type without mentally parsing indentation.

This approach keeps developers close to the file — they remain aware they're editing a real YAML artifact, PRs look normal, and the git history stays clean.

### Schema-driven autocompletion

Prathya publishes a **JSON Schema** for CONTRACT.yaml. IntelliJ has built-in support for JSON Schema on YAML files — once registered, the editor provides:

- Field name autocompletion as you type
- Value autocompletion for enum fields — `status` offers `draft`, `approved`, `deprecated`, `superseded`
- Required field validation — missing mandatory fields underlined immediately
- Type validation — wrong field types surfaced inline
- Hover documentation — schema description shown on hover over any field
- ID format validation via regex — `^[A-Z]+-[0-9]{3}(-CC-[0-9]{3})?$`
- Semver format enforcement on version fields
- Date format enforcement on changelog entries

**Schema distribution — two channels:**

The schema is published to **SchemaStore.org** — IntelliJ, VS Code, and any SchemaStore-aware editor automatically picks it up for files named `CONTRACT.yaml` with zero user configuration. This gives VS Code users autocompletion and validation for free without any plugin, extending Prathya's reach beyond IntelliJ significantly.

The schema is also **bundled in the plugin** and registered programmatically as a fallback — works without internet access and guarantees the schema version matches the plugin version.

Cross-reference validation — `supersedes` and `superseded_by` pointing to valid existing IDs — requires dynamic plugin logic beyond what static JSON Schema can express and is handled separately by the plugin.

### Versioning assistant

When an existing requirement is edited in split mode, the plugin detects change severity and prompts — breaking change suggests a major bump, clarification suggests a patch. Changelog entries are generated automatically with the current date. The developer just confirms.

### Traceability gutter icons

In test files, a gutter icon appears next to `@Requirement("AUTH-001")` annotations. Hovering shows the requirement title, description, and status inline without leaving the test file. Clicking navigates to the requirement in CONTRACT.yaml. If the requirement is deprecated or superseded the icon shows a warning colour.

The reverse works too — in rendered mode, each requirement card shows which test methods are mapped to it with their current pass/fail state from the last `prathya:run` execution. Clicking a test method name navigates directly to it.

### Inline audit warnings

- `@Requirement` references an ID not found in CONTRACT.yaml
- `@Requirement` references a deprecated or superseded requirement
- Approved requirement exists with no mapped tests

### `prathya:run` integration

A run button on each requirement card in rendered mode triggers `prathya:run -Dprathya.requirementId=<ID>` for that specific requirement directly from the IDE. Results update the coverage state on the card in real time.

**Distribution:** JetBrains Marketplace. JSON Schema published to SchemaStore.org independently of the plugin release cycle.

---

## Roadmap

1. **v0.1** — `prathya-annotations` + `prathya-core` (parser, scanner, coverage computation)
2. **v0.2** — `prathya-maven-plugin` — `prathya:verify` goal, HTML + JSON report via JMustache
3. **v0.3** — CI gate (fail build on violations, minimum coverage threshold)
4. **v0.4** — `prathya:run` focused test runner — Surefire-based, three-state coverage
5. **v0.5** — Failsafe support in `prathya:run` for integration test execution
6. **v0.6** — Multi-module aggregation (`prathya:aggregate`)
7. **v0.7** — `prathya-gradle-plugin`
8. **v0.8** — `prathya-intellij-plugin` — requirement authoring UI, traceability gutter icons, validation
9. **v1.0** — Maven Central publication, stable schema, production-ready

### Future considerations (post v1.0)
- **Test generation** — given a `CONTRACT.yaml`, generate test method stubs pre-annotated with `@Requirement` IDs, one per requirement and corner case. Reduces the bootstrap cost of writing contract tests from scratch and ensures no requirement or corner case is missed. Natural fit for AI-assisted generation in the vibe coding workflow.
- **MCP server** — expose Prathya's full read/write surface as a Model Context Protocol server. Tools like `get_contract`, `list_requirements`, `get_requirement`, `list_untested`, `get_coverage_matrix`, `add_requirement`, `update_requirement`, `add_corner_case` allow AI agents to participate directly in the contract-driven development loop. The agent reads the contract before generating code, checks coverage after generating tests, and iterates until the contract is satisfied — closing the vibe coding quality loop that no existing tool addresses.

---

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
| Test result parsing | Surefire XML (`TEST-*.xml`) — standard JUnit format |
| Distribution | Maven Central |
| Source | GitHub under `intrigsoft` org |
| License | Apache 2.0 |