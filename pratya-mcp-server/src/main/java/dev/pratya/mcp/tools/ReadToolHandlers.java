package dev.pratya.mcp.tools;

import dev.pratya.core.PratyaException;
import dev.pratya.core.audit.AuditEngine;
import dev.pratya.core.audit.DefaultAuditEngine;
import dev.pratya.core.coverage.CoverageComputer;
import dev.pratya.core.coverage.DefaultCoverageComputer;
import dev.pratya.core.model.*;
import dev.pratya.core.parser.RequirementParser;
import dev.pratya.core.parser.YamlRequirementParser;
import dev.pratya.core.scanner.AnnotationScanner;
import dev.pratya.core.scanner.ReflectionAnnotationScanner;
import dev.pratya.mcp.PratyaServerConfig;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handlers for the 7 read-only MCP tools.
 */
public class ReadToolHandlers {

    private final PratyaServerConfig config;
    private final RequirementParser parser = new YamlRequirementParser();
    private final CoverageComputer coverageComputer = new DefaultCoverageComputer();
    private final AuditEngine auditEngine = new DefaultAuditEngine();
    private final AnnotationScanner scanner = new ReflectionAnnotationScanner();

    public ReadToolHandlers(PratyaServerConfig config) {
        this.config = config;
    }

    // ── get_contract ──

    public McpSchema.CallToolResult getContract(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
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
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── list_requirements ──

    public McpSchema.CallToolResult listRequirements(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
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
            ModuleContract contract = loadContract();
            RequirementDefinition req = contract.getRequirements().stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new PratyaException("Requirement not found: " + id));

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
                    sb.append("  ").append(cc.getId()).append(": ").append(cc.getDescription()).append("\n");
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
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── list_untested ──

    public McpSchema.CallToolResult listUntested(Map<String, Object> args) {
        try {
            ModuleContract contract = loadContract();
            List<TraceEntry> traces = scanTraces();
            CoverageMatrix matrix = coverageComputer.compute(contract, traces);

            List<String> untested = new ArrayList<>();
            for (RequirementCoverage rc : matrix.getRequirements()) {
                if (!rc.isCovered() && (rc.getStatus() == RequirementStatus.APPROVED
                        || rc.getStatus() == RequirementStatus.DRAFT)) {
                    untested.add(rc.getId());
                }
            }

            if (untested.isEmpty()) {
                return textResult("All active requirements have tests.");
            }
            StringBuilder sb = new StringBuilder("Untested requirements:\n");
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
            ModuleContract contract = loadContract();
            List<TraceEntry> traces = scanTraces();
            CoverageMatrix matrix = coverageComputer.compute(contract, traces);

            StringBuilder sb = new StringBuilder();
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
                      .append(ccc.isCovered() ? "COVERED" : "UNCOVERED").append("\n");
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
            ModuleContract contract = loadContract();
            List<TraceEntry> traces = scanTraces();
            List<Violation> violations = auditEngine.audit(contract, traces);

            if (violations.isEmpty()) {
                return textResult("Audit passed — no violations found.");
            }
            StringBuilder sb = new StringBuilder("Audit found " + violations.size() + " violation(s):\n");
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
            ModuleContract contract = loadContract();
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
        } catch (PratyaException e) {
            return errorResult(e.getMessage());
        }
    }

    // ── helpers ──

    private ModuleContract loadContract() throws PratyaException {
        return parser.parse(config.getContractFile());
    }

    private List<TraceEntry> scanTraces() {
        if (config.getTestClassesDir() == null) {
            return List.of();
        }
        return scanner.scan(
                List.of(config.getTestClassesDir()),
                config.getAnnotationScanClasspath()
        );
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

    private static String requireStringArg(Map<String, Object> args, String key) throws PratyaException {
        String v = stringArg(args, key);
        if (v == null || v.isBlank()) {
            throw new PratyaException("Missing required argument: " + key);
        }
        return v;
    }
}
