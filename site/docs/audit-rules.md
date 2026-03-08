# Audit Rules

The `prathya:audit` goal (and the audit phase within `prathya:verify`) checks your contract and test mappings for issues.

## Rules

| Rule | Severity | Trigger | Example |
|---|---|---|---|
| Unknown requirement ID | ERROR | `@Requirement` references an ID not found in CONTRACT.yaml | `@Requirement("FOO-999")` but `FOO-999` not in contract |
| Uncovered approved requirement | ERROR | An `approved` requirement has zero tests | `AUTH-001` is approved but no test has `@Requirement("AUTH-001")` |
| Uncovered corner case | WARN | An `approved` requirement has corner cases without tests | `AUTH-001-CC-003` has no mapped test |
| Deprecated requirement referenced | WARN | A test still references a `deprecated` requirement | `@Requirement("AUTH-003")` where `AUTH-003` is deprecated |
| Superseded requirement referenced | WARN | A test still references a `superseded` requirement | `@Requirement("AUTH-003")` where `AUTH-003` is superseded by `AUTH-005` |

## Severity Levels

| Severity | Build Impact |
|---|---|
| **ERROR** | Fails the build when `failOnViolations` is true |
| **WARN** | Printed to console but does not fail the build |

## Configuration

### Enable/disable build failure

```xml
<configuration>
    <failOnViolations>true</failOnViolations>
</configuration>
```

### Exclude statuses from coverage calculations

```xml
<configuration>
    <excludeStatuses>
        <status>draft</status>
        <status>deprecated</status>
    </excludeStatuses>
</configuration>
```

## CI Integration

Typical CI setup:

```xml
<plugin>
    <groupId>com.intrigsoft.prathya</groupId>
    <artifactId>prathya-maven-plugin</artifactId>
    <version>${prathya.version}</version>
    <configuration>
        <failOnViolations>true</failOnViolations>
    </configuration>
    <executions>
        <execution>
            <goals><goal>verify</goal></goals>
        </execution>
    </executions>
</plugin>
```

The build fails if any of the following are detected:

- `@Requirement` annotation references an ID not in `CONTRACT.yaml`
- An `approved` requirement has zero test coverage
- An `approved` corner case has zero test coverage

## JSON Report

Violations are also included in the JSON report at `target/prathya/prathya-report.json`:

```json
{
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
