# Gradle Plugin

!!! warning "In Development"
    The Gradle plugin is on the [roadmap](roadmap.md) as v0.7. This page documents the planned interface.

## Setup

### Apply the plugin (Kotlin DSL)

```kotlin
plugins {
    id("com.intrigsoft.prathya.gradle") version "0.6.1-SNAPSHOT"
}

dependencies {
    testImplementation("com.intrigsoft.prathya:prathya-annotations:0.6.1-SNAPSHOT")
}
```

### Configuration

```kotlin
prathya {
    contractFile = file("CONTRACT.yaml")
    failOnViolations = true
    outputDirectory = layout.buildDirectory.dir("prathya")
}
```

## Tasks

| Task | Description |
|---|---|
| `prathyaVerify` | Full pipeline: parse contract, scan tests, compute coverage, audit, generate reports |
| `prathyaAudit` | Run the audit engine only — reports violations to console |
| `prathyaReport` | Generate reports only (no build failure) |
| `prathyaRun` | Run tests mapped to requirements |

## Usage

```bash
# Full verification
./gradlew prathyaVerify

# Audit only
./gradlew prathyaAudit

# Generate reports
./gradlew prathyaReport
```

The `prathyaVerify` task depends on the `test` task and runs after tests complete, mirroring the Maven lifecycle.

## Output

Reports are generated in `build/prathya/`:

| Output | Location |
|---|---|
| HTML report | `build/prathya/index.html` |
| JSON report | `build/prathya/prathya-report.json` |
