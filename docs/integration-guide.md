# Prathya Integration Guide

Add contract-driven requirement coverage to any Maven project. Prathya scans your tests for `@Requirement` annotations, matches them against a `CONTRACT.yaml`, and generates coverage reports showing which requirements have tests, which pass, and (optionally) how that correlates with JaCoCo code coverage.

## Prerequisites

- Java 17+
- Maven 3.8+
- Prathya artifacts installed to your local Maven repository (or published to a private registry)

```bash
# From the prathya repo root
mvn install -DskipTests
```

This installs `prathya-annotations`, `prathya-core`, and `prathya-maven-plugin` version `0.6.0`.

---

## Step 1: Add Dependencies and Plugins

Add the following to your project's `pom.xml`:

```xml
<properties>
    <prathya.version>0.6.0</prathya.version>
</properties>

<dependencies>
    <!-- Prathya test annotation (test scope only, zero transitive deps) -->
    <dependency>
        <groupId>com.intrigsoft.prathya</groupId>
        <artifactId>prathya-annotations</artifactId>
        <version>${prathya.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- (Optional) JaCoCo for code coverage in the Prathya report -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>

        <!-- Prathya plugin -->
        <plugin>
            <groupId>com.intrigsoft.prathya</groupId>
            <artifactId>prathya-maven-plugin</artifactId>
            <version>${prathya.version}</version>
            <executions>
                <execution>
                    <goals><goal>verify</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

> JaCoCo `report` runs in the `test` phase, which completes before Prathya `verify` runs in the `verify` phase. No special ordering needed.

---

## Step 2: Create CONTRACT.yaml

Create `CONTRACT.yaml` in your project root (next to `pom.xml`).

### Requirement ID Convention

| Pattern | Example | Meaning |
|---------|---------|---------|
| `{MODULE}-{NNN}` | `AUTH-001` | Requirement |
| `{MODULE}-{NNN}-CC-{N}` | `AUTH-001-CC-001` | Corner case |

IDs are append-only and never reused. Requirements are never deleted — mark them `deprecated` or `superseded`.

### Statuses

| Status | Meaning |
|--------|---------|
| `draft` | Not yet approved — excluded from coverage calculations |
| `approved` | Active requirement — must have tests |
| `deprecated` | No longer relevant — excluded from coverage |
| `superseded` | Replaced by a newer requirement |

### Example

```yaml
module:
  id: AUTH
  name: Auth Service
  description: Multi-tenant authentication and authorization

requirements:
  - id: AUTH-001
    version: 1.0.0
    status: approved
    title: User login with valid credentials
    description: >
      The system must authenticate a user given valid userId and password,
      returning a signed JWT.
    acceptance_criteria:
      - Returns HTTP 200 with a JWT in the response body
      - JWT contains tenantId, role, and permissions claims
    corner_cases:
      - id: AUTH-001-CC-001
        description: Invalid password — must return 401
      - id: AUTH-001-CC-002
        description: Deactivated user — must return 403

  - id: AUTH-002
    version: 1.0.0
    status: approved
    title: Reject expired JWT
    description: >
      The resource server must reject requests bearing an expired JWT.
    acceptance_criteria:
      - Returns HTTP 401 for expired tokens
```

---

## Step 3: Annotate Tests

Import `@Requirement` and annotate each test method with the IDs it verifies:

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

    @Test
    @Requirement({"AUTH-001", "AUTH-002"})
    void login_multipleRequirements() {
        // a single test can map to multiple requirement IDs
    }
}
```

- `@Requirement` accepts a single ID or an array of IDs.
- Place it on test methods (`@Test`), not on classes.
- Corner case IDs (e.g., `AUTH-001-CC-001`) are annotated the same way as requirement IDs.

---

## Step 4: Run

```bash
mvn clean verify
```

Prathya runs after tests complete and produces:

| Output | Location |
|--------|----------|
| HTML report | `target/prathya/index.html` |
| JSON report | `target/prathya/prathya-report.json` |
| Console summary | Maven log output |

Console output example:

```
[INFO] Prathya Report: AUTH
[INFO]   Requirements: 5/6 covered (83.3%)
[INFO]   Corner cases: 3/4 covered (75.0%)
[INFO]   Violations: 1 (1 error, 0 warn)
[INFO]   Code coverage: 87.2% lines, 71.4% branches
[INFO]   Reports: /path/to/target/prathya
```

The HTML report shows summary cards for requirement coverage, corner case coverage, and (if JaCoCo is present) line and branch coverage. Hover the info icon on each card for an explanation.

---

## Configuration Reference

All parameters can be set in `<configuration>` or as `-D` system properties.

| Property | Default | Description |
|----------|---------|-------------|
| `prathya.contractFile` | `${project.basedir}/CONTRACT.yaml` | Path to the contract file |
| `prathya.skip` | `false` | Skip Prathya execution entirely |
| `prathya.failOnViolations` | `true` | Fail the build on audit violations |
| `prathya.minimumRequirementCoverage` | `0` | Minimum requirement coverage % (0 = no threshold) |
| `prathya.minimumCornerCaseCoverage` | `0` | Minimum corner case coverage % (0 = no threshold) |
| `prathya.jacocoReportFile` | `${project.build.directory}/site/jacoco/jacoco.xml` | Path to JaCoCo XML report |
| `prathya.outputDirectory` | `${project.build.directory}/prathya` | Report output directory |

### Example: CI Gate with Coverage Thresholds

```xml
<plugin>
    <groupId>com.intrigsoft.prathya</groupId>
    <artifactId>prathya-maven-plugin</artifactId>
    <version>${prathya.version}</version>
    <configuration>
        <failOnViolations>true</failOnViolations>
        <minimumRequirementCoverage>80</minimumRequirementCoverage>
        <minimumCornerCaseCoverage>60</minimumCornerCaseCoverage>
    </configuration>
    <executions>
        <execution>
            <goals><goal>verify</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Example: Exclude Draft Requirements

```xml
<configuration>
    <excludeStatuses>
        <status>draft</status>
        <status>deprecated</status>
    </excludeStatuses>
</configuration>
```

---

## Plugin Goals

| Goal | Phase | Description |
|------|-------|-------------|
| `prathya:verify` | `verify` | Full pipeline: parse contract, scan tests, compute coverage, audit, generate reports. Fails the build if `failOnViolations` is true and violations exist. |
| `prathya:audit` | (manual) | Run the audit engine only — reports violations without generating HTML/JSON. |
| `prathya:report` | `verify` | Generate reports only (no build failure on violations). |
| `prathya:run` | (manual) | Run tests for a specific requirement: `mvn prathya:run -Dprathya.requirementId=AUTH-001` |
| `prathya:aggregate` | `verify` | Aggregate reports across a multi-module reactor. |

---

## Audit Rules

Prathya audits your contract and test mappings and reports violations:

| Rule | Severity | Trigger |
|------|----------|---------|
| Unknown requirement ID | ERROR | `@Requirement("FOO-999")` but `FOO-999` not in CONTRACT.yaml |
| Uncovered approved requirement | ERROR | An `approved` requirement has zero tests |
| Uncovered corner case | WARN | An `approved` requirement has corner cases without tests |
| Deprecated requirement referenced | WARN | A test still references a `deprecated` requirement |
| Superseded requirement referenced | WARN | A test still references a `superseded` requirement |
| Coverage below threshold | ERROR | Requirement or corner case coverage below configured minimum |

---

## Multi-Module Projects

For multi-module Maven projects, each module gets its own `CONTRACT.yaml` and `@Requirement` annotations. Add the `aggregate` goal to the parent POM to get a combined report:

```xml
<!-- Parent POM -->
<plugin>
    <groupId>com.intrigsoft.prathya</groupId>
    <artifactId>prathya-maven-plugin</artifactId>
    <version>${prathya.version}</version>
    <executions>
        <execution>
            <goals><goal>aggregate</goal></goals>
        </execution>
    </executions>
</plugin>
```

Aggregate report output: `target/prathya-aggregate/index.html`

---

## Interpreting the Report

The HTML report shows four summary cards (two more when JaCoCo is present):

| Card | What it means |
|------|---------------|
| **Requirement Coverage** | % of approved requirements with at least one `@Requirement`-annotated test |
| **Corner Case Coverage** | % of defined corner cases with at least one mapped test |
| **Line Coverage** | % of executable lines hit by tests (from JaCoCo) |
| **Branch Coverage** | % of code branches (if/else, switch) hit by tests (from JaCoCo) |

### Cross-referencing Requirement and Code Coverage

| Requirement Coverage | Code Coverage | Interpretation |
|---------------------|---------------|----------------|
| High | High | Well-tested and well-documented |
| High | Low | Requirements are mapped but tests may be shallow |
| Low | High | Code is exercised but features aren't documented in the contract — possible dead code or undocumented features |
| Low | Low | Undertested and underdocumented |
