package com.intrigsoft.prathya.mcp.tools;

import com.intrigsoft.prathya.core.PrathyaException;
import com.intrigsoft.prathya.core.audit.AuditEngine;
import com.intrigsoft.prathya.core.audit.DefaultAuditEngine;
import com.intrigsoft.prathya.core.coverage.CoverageComputer;
import com.intrigsoft.prathya.core.coverage.DefaultCoverageComputer;
import com.intrigsoft.prathya.core.model.*;
import com.intrigsoft.prathya.core.parser.RequirementParser;
import com.intrigsoft.prathya.core.parser.YamlRequirementParser;
import com.intrigsoft.prathya.core.scanner.AnnotationScanner;
import com.intrigsoft.prathya.core.scanner.BytecodeAnnotationScanner;
import com.intrigsoft.prathya.core.scanner.ReflectionAnnotationScanner;
import com.intrigsoft.prathya.mcp.PrathyaServerConfig;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handlers for the 8 read-only MCP tools.
 */
public class ReadToolHandlers {

    private final PrathyaServerConfig config;
    private final RequirementParser parser = new YamlRequirementParser();
    private final CoverageComputer coverageComputer = new DefaultCoverageComputer();
    private final AuditEngine auditEngine = new DefaultAuditEngine();
    private final AnnotationScanner scanner = new BytecodeAnnotationScanner();

    public ReadToolHandlers(PrathyaServerConfig config) {
        this.config = config;
    }

    // ── get_contract ──

    public McpSchema.CallToolResult getContract(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
            StringBuilder sb = new StringBuilder();
            sb.append("Module: ").append(contract.getModule().getId())
              .append(" — ").append(contract.getModule().getName()).append("\n");
            if (contract.getModule().getDescription() != null) {
                sb.append("Description: ").append(contract.getModule().getDescription()).append("\n");
            }
            if (contract.getModule().getOwner() != null) {
                sb.append("Owner: ").append(contract.getModule().getOwner()).append("\n");
            }
            if (contract.getModule().getVersion() != null) {
                sb.append("Version: ").append(contract.getModule().getVersion()).append("\n");
            }
            sb.append("\nRequirements: ").append(contract.getRequirements().size()).append("\n");
            for (RequirementDefinition req : contract.getRequirements()) {
                sb.append("  ").append(req.getId()).append(" [").append(req.getStatus()).append("] ")
                  .append(req.getTitle()).append("\n");
            }
            return textResult(sb.toString());
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── list_requirements ──

    public McpSchema.CallToolResult listRequirements(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
            List<RequirementDefinition> reqs = contract.getRequirements();

            String statusFilter = stringArg(args, "status");
            if (statusFilter != null) {
                RequirementStatus filter = RequirementStatus.valueOf(statusFilter.toUpperCase());
                reqs = reqs.stream().filter(r -> r.getStatus() == filter).collect(Collectors.toList());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Requirements (").append(reqs.size()).append("):\n");
            for (RequirementDefinition req : reqs) {
                sb.append("  ").append(req.getId()).append(" [").append(req.getStatus()).append("] ")
                  .append(req.getTitle());
                if (!req.getCornerCases().isEmpty()) {
                    sb.append(" (").append(req.getCornerCases().size()).append(" corner cases)");
                }
                sb.append("\n");
            }
            return textResult(sb.toString());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ── get_requirement ──

    public McpSchema.CallToolResult getRequirement(Map<String, Object> args) {
        try {
            String id = requireStringArg(args, "id");
            ModuleContract contract = loadContract(args);
            RequirementDefinition req = contract.getRequirements().stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new PrathyaException("Requirement not found: " + id));

            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(req.getId()).append("\n");
            sb.append("Title: ").append(req.getTitle()).append("\n");
            sb.append("Status: ").append(req.getStatus()).append("\n");
            if (req.getVersion() != null) sb.append("Version: ").append(req.getVersion()).append("\n");
            if (req.getDescription() != null) sb.append("Description: ").append(req.getDescription()).append("\n");
            if (req.getSupersedes() != null) sb.append("Supersedes: ").append(req.getSupersedes()).append("\n");
            if (req.getSupersededBy() != null) sb.append("Superseded by: ").append(req.getSupersededBy()).append("\n");

            if (!req.getAcceptanceCriteria().isEmpty()) {
                sb.append("Acceptance Criteria:\n");
                for (String ac : req.getAcceptanceCriteria()) {
                    sb.append("  - ").append(ac).append("\n");
                }
            }
            if (!req.getCornerCases().isEmpty()) {
                sb.append("Corner Cases:\n");
                for (CornerCase cc : req.getCornerCases()) {
                    sb.append("  ").append(cc.getId()).append(": ").append(cc.getDescription());
                    if (cc.getTestEnvironment() != null) {
                        sb.append(" [env: ").append(cc.getTestEnvironment().toYaml()).append("]");
                    }
                    sb.append("\n");
                }
            }
            if (req.getChangelog() != null && !req.getChangelog().isEmpty()) {
                sb.append("Changelog:\n");
                for (ChangelogEntry entry : req.getChangelog()) {
                    sb.append("  ");
                    if (entry.getVersion() != null) sb.append("[").append(entry.getVersion()).append("] ");
                    if (entry.getDate() != null) sb.append(entry.getDate()).append(" ");
                    sb.append(entry.getNote()).append("\n");
                }
            }
            return textResult(sb.toString());
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── list_untested ──

    public McpSchema.CallToolResult listUntested(Map<String, Object> args) {
        try {
            Path contractPath = resolveContractFile(args);
            String warning = scanWarning(contractPath);
            ModuleContract contract = parser.parse(contractPath);
            List<TraceEntry> traces = scanTraces(contractPath);
            CoverageMatrix matrix = coverageComputer.compute(contract, traces);

            List<String> untested = new ArrayList<>();
            for (RequirementCoverage rc : matrix.getRequirements()) {
                if (!rc.isCovered() && (rc.getStatus() == RequirementStatus.APPROVED
                        || rc.getStatus() == RequirementStatus.DRAFT)) {
                    untested.add(rc.getId());
                }
            }

            if (untested.isEmpty()) {
                return textResult(warning + "All active requirements have tests.");
            }
            StringBuilder sb = new StringBuilder(warning).append("Untested requirements:\n");
            for (String id : untested) {
                sb.append("  ").append(id).append("\n");
            }
            return textResult(sb.toString());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ── get_coverage_matrix ──

    public McpSchema.CallToolResult getCoverageMatrix(Map<String, Object> args) {
        try {
            Path contractPath = resolveContractFile(args);
            String warning = scanWarning(contractPath);
            ModuleContract contract = parser.parse(contractPath);
            List<TraceEntry> traces = scanTraces(contractPath);
            CoverageMatrix matrix = coverageComputer.compute(contract, traces);

            StringBuilder sb = new StringBuilder(warning);
            CoverageSummary summary = matrix.getSummary();
            sb.append("Coverage Summary:\n");
            sb.append("  Requirements: ").append(summary.getCoveredRequirements())
              .append("/").append(summary.getActiveRequirements())
              .append(" (").append(String.format("%.1f%%", summary.getRequirementCoverage())).append(")\n");
            sb.append("  Corner Cases: ").append(summary.getCoveredCornerCases())
              .append("/").append(summary.getTotalCornerCases())
              .append(" (").append(String.format("%.1f%%", summary.getCornerCaseCoverage())).append(")\n\n");

            for (RequirementCoverage rc : matrix.getRequirements()) {
                sb.append(rc.getId()).append(" [").append(rc.getStatus()).append("] ")
                  .append(rc.isCovered() ? "COVERED" : "UNCOVERED");
                if (!rc.getTests().isEmpty()) {
                    sb.append(" — tests: ").append(String.join(", ", rc.getTests()));
                }
                sb.append("\n");
                for (CornerCaseCoverage ccc : rc.getCornerCases()) {
                    sb.append("  ").append(ccc.getId()).append(": ")
                      .append(ccc.isCovered() ? "COVERED" : "UNCOVERED");
                    if (ccc.getTestEnvironment() != null) {
                        sb.append(" [env: ").append(ccc.getTestEnvironment()).append("]");
                    }
                    sb.append("\n");
                }
            }
            return textResult(sb.toString());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ── run_audit ──

    public McpSchema.CallToolResult runAudit(Map<String, Object> args) {
        try {
            Path contractPath = resolveContractFile(args);
            String warning = scanWarning(contractPath);
            ModuleContract contract = parser.parse(contractPath);
            List<TraceEntry> traces = scanTraces(contractPath);
            List<Violation> violations = auditEngine.audit(contract, traces);

            if (violations.isEmpty()) {
                return textResult(warning + "Audit passed — no violations found.");
            }
            StringBuilder sb = new StringBuilder(warning).append("Audit found " + violations.size() + " violation(s):\n");
            for (Violation v : violations) {
                sb.append("  [").append(v.getType().getSeverity()).append("] ")
                  .append(v.getType()).append(": ").append(v.getMessage()).append("\n");
            }
            return textResult(sb.toString());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ── validate_contract ──

    public McpSchema.CallToolResult validateContract(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract(args);
            List<String> issues = new ArrayList<>();

            Set<String> ids = new HashSet<>();
            for (RequirementDefinition req : contract.getRequirements()) {
                if (!ids.add(req.getId())) {
                    issues.add("Duplicate requirement ID: " + req.getId());
                }
                if (req.getTitle() == null || req.getTitle().isBlank()) {
                    issues.add(req.getId() + ": missing title");
                }
                if (!ContractConstants.REQ_ID_PATTERN.matcher(req.getId()).matches()) {
                    issues.add(req.getId() + ": invalid ID format");
                }
                Set<String> ccIds = new HashSet<>();
                for (CornerCase cc : req.getCornerCases()) {
                    if (!ccIds.add(cc.getId())) {
                        issues.add("Duplicate corner case ID: " + cc.getId());
                    }
                    if (!ContractConstants.CC_ID_PATTERN.matcher(cc.getId()).matches()) {
                        issues.add(cc.getId() + ": invalid corner case ID format");
                    }
                }
                if (req.getStatus() == RequirementStatus.SUPERSEDED && req.getSupersededBy() == null) {
                    issues.add(req.getId() + ": SUPERSEDED but missing superseded_by");
                }
            }

            if (issues.isEmpty()) {
                return textResult("Contract is valid. " + contract.getRequirements().size() + " requirements checked.");
            }
            StringBuilder sb = new StringBuilder("Validation found " + issues.size() + " issue(s):\n");
            for (String issue : issues) {
                sb.append("  - ").append(issue).append("\n");
            }
            return textResult(sb.toString());
        } catch (PrathyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── configure_project ──

    public McpSchema.CallToolResult configureProject(Map<String, Object> args) {
        String buildTool = stringArg(args, "build_tool");
        String version = stringArg(args, "version");
        if (version == null) version = "0.6.1";

        // Auto-detect build tool if not specified
        if (buildTool == null) {
            if (Files.exists(Path.of("pom.xml"))) {
                buildTool = "maven";
            } else if (Files.exists(Path.of("build.gradle.kts")) || Files.exists(Path.of("build.gradle"))) {
                buildTool = "gradle";
            } else {
                return errorResult(
                        "Could not auto-detect build tool. No pom.xml or build.gradle.kts found in working directory. " +
                        "Specify build_tool explicitly as 'maven' or 'gradle'.");
            }
        }

        if ("maven".equalsIgnoreCase(buildTool)) {
            return textResult(mavenConfiguration(version));
        } else if ("gradle".equalsIgnoreCase(buildTool)) {
            return textResult(gradleConfiguration(version));
        } else {
            return errorResult("Unsupported build tool: " + buildTool + ". Use 'maven' or 'gradle'.");
        }
    }

    private String mavenConfiguration(String version) {
        return """
                # Prathya Maven Configuration Guide

                ## Step 1: Add the annotation dependency

                Add to your `<dependencies>` section in pom.xml:

                ```xml
                <dependency>
                    <groupId>com.intrigsoft.prathya</groupId>
                    <artifactId>prathya-annotations</artifactId>
                    <version>%1$s</version>
                    <scope>test</scope>
                </dependency>
                ```

                ## Step 2: Add the Prathya Maven plugin

                Add to your `<build><plugins>` section:

                ```xml
                <plugin>
                    <groupId>com.intrigsoft.prathya</groupId>
                    <artifactId>prathya-maven-plugin</artifactId>
                    <version>%1$s</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>verify</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                ```

                ## Step 3: Add JaCoCo for code coverage correlation (recommended)

                JaCoCo lets Prathya show code coverage alongside requirement coverage.
                Add to your `<build><plugins>` section:

                ```xml
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>0.8.12</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>prepare-agent</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>report</id>
                            <phase>test</phase>
                            <goals>
                                <goal>report</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                ```

                JaCoCo `report` runs in the `test` phase, which completes before Prathya `verify` \
                runs in the `verify` phase. No special ordering needed.

                Prathya automatically reads the JaCoCo XML report from: \
                `target/site/jacoco/jacoco.xml`

                ## Step 4: Create CONTRACT.yaml

                Create a `CONTRACT.yaml` file in your project root (next to pom.xml). Example:

                ```yaml
                module:
                  id: MY-MODULE
                  name: My Module
                  version: "1.0"

                requirements: []
                ```

                Then use `add_requirement` to add requirements to the contract.

                ## Step 5: Run

                ```bash
                mvn clean verify
                ```

                Prathya will parse CONTRACT.yaml, scan test classes for @Requirement annotations, \
                compute coverage, run audit, and generate reports in `target/prathya/`.

                ## Optional: Coverage thresholds

                Add `<configuration>` to the Prathya plugin to enforce minimum coverage:

                ```xml
                <configuration>
                    <minimumRequirementCoverage>80</minimumRequirementCoverage>
                    <minimumCornerCaseCoverage>60</minimumCornerCaseCoverage>
                </configuration>
                ```

                ## Plugin goals reference

                | Goal | Phase | Description |
                |------|-------|-------------|
                | `verify` | verify | Full pipeline: parse, scan, coverage, audit, reports. Fails on violations. |
                | `audit` | manual | Audit only — reports violations without generating reports. |
                | `report` | verify | Generate reports only (no build failure on violations). |
                | `run` | manual | Run tests mapped to specific requirements via Surefire. |
                | `aggregate` | verify | Aggregate reports across multi-module reactor. |
                """.formatted(version);
    }

    private String gradleConfiguration(String version) {
        return """
                # Prathya Gradle Configuration Guide

                ## Step 1: Apply the plugin and add the annotation dependency

                In your `build.gradle.kts`:

                ```kotlin
                plugins {
                    java
                    id("com.intrigsoft.prathya") version "%1$s"
                }

                dependencies {
                    testImplementation("com.intrigsoft.prathya:prathya-annotations:%1$s")
                }
                ```

                Or in `build.gradle` (Groovy DSL):

                ```groovy
                plugins {
                    id 'java'
                    id 'com.intrigsoft.prathya' version '%1$s'
                }

                dependencies {
                    testImplementation 'com.intrigsoft.prathya:prathya-annotations:%1$s'
                }
                ```

                ## Step 2: Configure Prathya

                ```kotlin
                prathya {
                    failOnViolations.set(true)
                }
                ```

                ## Step 3: Add JaCoCo for code coverage correlation (recommended)

                ```kotlin
                plugins {
                    jacoco
                }

                tasks.test {
                    useJUnitPlatform()
                    finalizedBy(tasks.jacocoTestReport)
                }

                tasks.jacocoTestReport {
                    reports {
                        xml.required.set(true)
                    }
                }

                prathya {
                    jacocoReportFile.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
                }
                ```

                ## Step 4: Create CONTRACT.yaml

                Create a `CONTRACT.yaml` file in your project root (next to build.gradle.kts). Example:

                ```yaml
                module:
                  id: MY-MODULE
                  name: My Module
                  version: "1.0"

                requirements: []
                ```

                Then use `add_requirement` to add requirements to the contract.

                ## Step 5: Run

                ```bash
                gradle prathyaVerify
                ```

                ## Available tasks

                | Task | Description |
                |------|-------------|
                | `prathyaVerify` | Full pipeline: parse, scan, coverage, audit, reports |
                | `prathyaAudit` | Audit only — reports violations to console |
                | `prathyaReport` | Generate reports only (no build failure) |
                | `prathyaRun` | Run tests mapped to requirements |

                ## Output locations

                | Output | Location |
                |--------|----------|
                | HTML report | `build/prathya/index.html` |
                | JSON report | `build/prathya/prathya-report.json` |
                """.formatted(version);
    }

    // ── helpers ──

    private ModuleContract loadContract(Map<String, Object> args) throws PrathyaException {
        return parser.parse(resolveContractFile(args));
    }

    private Path resolveContractFile(Map<String, Object> args) {
        String override = stringArg(args, "contract_file");
        if (override != null) {
            return Path.of(override);
        }
        return config.getContractFile();
    }

    /**
     * Resolves test-classes and classes dirs: uses config if explicitly set,
     * otherwise auto-detects from the resolved contract file path.
     */
    private Path[] resolveTestDirs(Path contractPath) {
        if (config.getTestClassesDir() != null) {
            return new Path[]{config.getTestClassesDir(),
                    config.getClassesDir()};
        }
        return PrathyaServerConfig.detectDirectories(contractPath);
    }

    private List<TraceEntry> scanTraces(Path contractPath) {
        Path[] dirs = resolveTestDirs(contractPath);
        Path testClassesDir = dirs[0];
        Path classesDir = dirs[1];
        if (testClassesDir == null) {
            return List.of();
        }
        List<Path> classpath = classesDir != null ? List.of(classesDir) : List.of();
        return scanner.scan(List.of(testClassesDir), classpath);
    }

    private String scanWarning(Path contractPath) {
        Path[] dirs = resolveTestDirs(contractPath);
        if (dirs[0] == null) {
            return "WARNING: No test-classes directory found. "
                    + "Annotation scanning is disabled — audit, coverage, and untested results will be empty. "
                    + "Use --test-classes to specify the directory, or build the project so that "
                    + "target/test-classes (Maven) or build/classes/java/test (Gradle) exists.\n\n";
        }
        return "";
    }

    static McpSchema.CallToolResult textResult(String text) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(text)
                .build();
    }

    static McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent("Error: " + message)
                .isError(true)
                .build();
    }

    private static String stringArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }

    private static String requireStringArg(Map<String, Object> args, String key) throws PrathyaException {
        String v = stringArg(args, key);
        if (v == null || v.isBlank()) {
            throw new PrathyaException("Missing required argument: " + key);
        }
        return v;
    }
}
