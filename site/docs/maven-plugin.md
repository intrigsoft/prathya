# Maven Plugin

## Prerequisites

- Java 17+
- Maven 3.8+
- Prathya artifacts installed to your local Maven repository (or published to a registry)

```bash
# From the prathya repo root
mvn install -DskipTests
```

## Setup

### 1. Add the annotation dependency

```xml
<properties>
    <prathya.version>0.6.0</prathya.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.intrigsoft.prathya</groupId>
        <artifactId>prathya-annotations</artifactId>
        <version>${prathya.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Add the plugin

```xml
<build>
    <plugins>
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

### Optional: JaCoCo integration

Prathya can read JaCoCo reports to correlate requirement coverage with code coverage.

```xml
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
```

!!! note
    JaCoCo `report` runs in the `test` phase, which completes before Prathya `verify` runs in the `verify` phase. No special ordering needed.

## Goals

| Goal | Phase | Description |
|---|---|---|
| `prathya:verify` | `verify` | Full pipeline: parse contract, scan tests, compute coverage, audit, generate reports. Fails the build if `failOnViolations` is true and violations exist. |
| `prathya:audit` | (manual) | Run the audit engine only — reports violations without generating HTML/JSON. |
| `prathya:report` | `verify` | Generate reports only (no build failure on violations). |
| `prathya:run` | (manual) | Run tests mapped to requirements via Surefire. |
| `prathya:aggregate` | `verify` | Aggregate reports across a multi-module reactor. |

## Configuration Reference

All parameters can be set in `<configuration>` or as `-D` system properties.

| Property | Default | Description |
|---|---|---|
| `prathya.contractFile` | `${project.basedir}/CONTRACT.yaml` | Path to the contract file |
| `prathya.skip` | `false` | Skip Prathya execution entirely |
| `prathya.failOnViolations` | `true` | Fail the build on audit violations |
| `prathya.jacocoReportFile` | `${project.build.directory}/site/jacoco/jacoco.xml` | Path to JaCoCo XML report |
| `prathya.outputDirectory` | `${project.build.directory}/prathya` | Report output directory |

## Exclude Statuses

```xml
<configuration>
    <excludeStatuses>
        <status>draft</status>
        <status>deprecated</status>
    </excludeStatuses>
</configuration>
```

## The `prathya:run` Goal

The `run` goal shifts Prathya from passive measurement to active execution. It resolves which test methods map to active requirements, delegates execution to Surefire, and parses the results.

### Execution Flow

1. Read `CONTRACT.yaml`
2. Scan `@Requirement` annotations across compiled test classes
3. Resolve test methods mapped to **approved** requirements
4. Construct a Surefire-compatible filter string
5. Invoke `mvn test -Dtest=<filter>` via Maven Invoker API
6. Parse `target/surefire-reports/TEST-*.xml`
7. Map results back to requirement IDs
8. Compute three-state coverage and generate report

### Usage

```bash
# Run all contract tests for approved requirements
mvn prathya:run

# Run tests for a specific requirement
mvn prathya:run -Dprathya.requirementId=AUTH-001

# Run tests for a specific module
mvn prathya:run -Dprathya.moduleId=AUTH
```

### Configuration

```xml
<configuration>
    <requirementId>AUTH-001</requirementId>
    <statusFilter>approved</statusFilter>
    <failOnTestFailure>true</failOnTestFailure>
    <surefireReportsDirectory>${project.build.directory}/surefire-reports</surefireReportsDirectory>
</configuration>
```

## Multi-Module Projects

Each module gets its own `CONTRACT.yaml`. Add the `aggregate` goal to the parent POM:

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

## Console Output

```
[INFO] Prathya Report: AUTH
[INFO]   Requirements: 5/6 covered (83.3%)
[INFO]   Corner cases: 3/4 covered (75.0%)
[INFO]   Violations: 1 (1 error, 0 warn)
[INFO]   Contract code coverage: 72.1% lines, 58.3% branches
[INFO]   Total code coverage: 87.2% lines, 71.4% branches
[INFO]   Reports: /path/to/target/prathya
```

## Report Interpretation

The HTML report shows summary cards:

| Card | Meaning |
|---|---|
| **Requirement Coverage** | % of approved requirements with at least one mapped test |
| **Corner Case Coverage** | % of defined corner cases with at least one mapped test |
| **Contract Code Coverage** | % of code covered by `@Requirement`-annotated tests only (from JaCoCo) |
| **Total Code Coverage** | % of all code covered by all tests (from JaCoCo) |

**Contract code coverage** is the key metric that bridges requirement traceability and code coverage. It answers: "how much of our code is exercised by tests that are linked to a requirement?" The gap between contract code coverage and total code coverage reveals how much test effort is untethered from the contract.
