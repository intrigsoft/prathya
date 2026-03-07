package dev.prathya.core.report;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import dev.prathya.core.model.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HtmlReportWriter implements ReportWriter {

    private static final String TEMPLATE_PATH = "/templates/prathya-report.mustache";
    private static final String AGGREGATE_TEMPLATE_PATH = "/templates/prathya-aggregate-report.mustache";

    @Override
    public void writeJsonReport(CoverageMatrix matrix, List<Violation> violations, Path outputFile) throws IOException {
        throw new UnsupportedOperationException("HtmlReportWriter does not support JSON output");
    }

    @Override
    public void writeHtmlReport(CoverageMatrix matrix, List<Violation> violations, Path outputDir) throws IOException {
        Template template = loadTemplate();
        Map<String, Object> viewModel = buildViewModel(matrix, violations);

        String html = template.execute(viewModel);

        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), html, StandardCharsets.UTF_8);
    }

    public void writeAggregateHtmlReport(AggregateReportData data, Path outputDir) throws IOException {
        Template template = loadTemplate(AGGREGATE_TEMPLATE_PATH);
        Map<String, Object> viewModel = buildAggregateViewModel(data);

        String html = template.execute(viewModel);

        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), html, StandardCharsets.UTF_8);
    }

    private Template loadTemplate() throws IOException {
        return loadTemplate(TEMPLATE_PATH);
    }

    private Template loadTemplate(String path) throws IOException {
        try (Reader reader = new InputStreamReader(
                getClass().getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return Mustache.compiler().nullValue("").compile(reader);
        }
    }

    private Map<String, Object> buildViewModel(CoverageMatrix matrix, List<Violation> violations) {
        Map<String, Object> model = new HashMap<>();

        model.put("moduleName", matrix.getModule().getName() != null
                ? matrix.getModule().getName() : matrix.getModule().getId());
        model.put("moduleId", matrix.getModule().getId());
        model.put("generatedAt", DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                .withZone(ZoneId.systemDefault()).format(Instant.now()));

        // Summary — merged contract coverage (requirements + corner cases as equal items)
        CoverageSummary s = matrix.getSummary();
        int coveredItems = s.getCoveredRequirements() + s.getCoveredCornerCases();
        int totalItems = s.getActiveRequirements() + s.getTotalCornerCases();
        double contractCoverage = totalItems == 0 ? 0.0 : (double) coveredItems / totalItems * 100;
        model.put("contractCoverage", formatPercent(contractCoverage));
        model.put("coveredItems", coveredItems);
        model.put("totalItems", totalItems);
        model.put("coveredRequirements", s.getCoveredRequirements());
        model.put("activeRequirements", s.getActiveRequirements());
        model.put("coveredCornerCases", s.getCoveredCornerCases());
        model.put("totalCornerCases", s.getTotalCornerCases());

        // Code coverage (JaCoCo)
        boolean hasCodeCoverage = matrix.getCodeCoverage() != null;
        model.put("hasCodeCoverage", hasCodeCoverage);
        if (hasCodeCoverage) {
            CodeCoverageSummary cc = matrix.getCodeCoverage();
            model.put("lineCoverage", formatPercent(cc.getLineRate()));
            model.put("branchCoverage", formatPercent(cc.getBranchRate()));
            model.put("lineCovered", cc.getLineCovered());
            model.put("lineMissed", cc.getLineMissed());
            model.put("lineCoveredTotal", cc.getLineCovered() + cc.getLineMissed());
            model.put("branchCovered", cc.getBranchCovered());
            model.put("branchMissed", cc.getBranchMissed());
            model.put("branchCoveredTotal", cc.getBranchCovered() + cc.getBranchMissed());
        }

        // Contract code coverage (JaCoCo — from requirement-mapped tests only)
        boolean hasContractCodeCoverage = matrix.getContractCodeCoverage() != null;
        model.put("hasContractCodeCoverage", hasContractCodeCoverage);
        if (hasContractCodeCoverage) {
            CodeCoverageSummary ccc = matrix.getContractCodeCoverage();
            model.put("contractLineCoverage", formatPercent(ccc.getLineRate()));
            model.put("contractBranchCoverage", formatPercent(ccc.getBranchRate()));
            model.put("contractLineCovered", ccc.getLineCovered());
            model.put("contractLineMissed", ccc.getLineMissed());
            model.put("contractLineCoveredTotal", ccc.getLineCovered() + ccc.getLineMissed());
            model.put("contractBranchCovered", ccc.getBranchCovered());
            model.put("contractBranchMissed", ccc.getBranchMissed());
            model.put("contractBranchCoveredTotal", ccc.getBranchCovered() + ccc.getBranchMissed());
        }

        // Build lookup from contract definitions (if available)
        Map<String, RequirementDefinition> defById = new HashMap<>();
        Map<String, CornerCase> ccDefById = new HashMap<>();
        if (matrix.getContract() != null) {
            for (RequirementDefinition def : matrix.getContract().getRequirements()) {
                defById.put(def.getId(), def);
                for (CornerCase cc : def.getCornerCases()) {
                    ccDefById.put(cc.getId(), cc);
                }
            }
        }

        // Group violations by requirement ID
        Set<String> knownReqIds = new HashSet<>();
        for (RequirementCoverage r : matrix.getRequirements()) {
            knownReqIds.add(r.getId());
        }
        Map<String, List<Violation>> violationsByReq = new HashMap<>();
        List<Violation> globalViolations = new ArrayList<>();
        for (Violation v : violations) {
            if (v.getRequirementId() != null && knownReqIds.contains(v.getRequirementId())) {
                violationsByReq.computeIfAbsent(v.getRequirementId(), k -> new ArrayList<>()).add(v);
            } else {
                globalViolations.add(v);
            }
        }

        // Requirements
        List<Map<String, Object>> reqs = new ArrayList<>();
        for (RequirementCoverage req : matrix.getRequirements()) {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("id", req.getId());
            reqMap.put("status", req.getStatus().name().toLowerCase());
            reqMap.put("coverageLabel", coverageLabel(req.isCovered(), req.getPassing()));
            reqMap.put("coverageClass", coverageClass(req.isCovered(), req.getPassing()));
            reqMap.put("testCount", req.getTests().size());
            reqMap.put("hasTests", !req.getTests().isEmpty());

            // Requirement definition metadata
            RequirementDefinition def = defById.get(req.getId());
            if (def != null) {
                reqMap.put("title", def.getTitle());
                reqMap.put("hasTitle", def.getTitle() != null && !def.getTitle().isEmpty());
                reqMap.put("description", def.getDescription());
                reqMap.put("hasDescription", def.getDescription() != null && !def.getDescription().isEmpty());
                List<String> ac = def.getAcceptanceCriteria();
                reqMap.put("acceptanceCriteria", ac);
                reqMap.put("hasAcceptanceCriteria", ac != null && !ac.isEmpty());
            }

            // Test details
            List<Map<String, Object>> testDetails = new ArrayList<>();
            for (String test : req.getTests()) {
                testDetails.add(buildTestDetail(test));
            }
            reqMap.put("testDetails", testDetails);

            // Corner cases
            reqMap.put("hasCornerCases", !req.getCornerCases().isEmpty());
            List<Map<String, Object>> cornerCases = new ArrayList<>();
            for (CornerCaseCoverage cc : req.getCornerCases()) {
                Map<String, Object> ccMap = new HashMap<>();
                ccMap.put("id", cc.getId());
                ccMap.put("covered", cc.isCovered());
                ccMap.put("uncovered", !cc.isCovered());
                ccMap.put("passing", cc.getPassing());

                CornerCase ccDef = ccDefById.get(cc.getId());
                if (ccDef != null) {
                    ccMap.put("description", ccDef.getDescription());
                    ccMap.put("hasDescription", ccDef.getDescription() != null && !ccDef.getDescription().isEmpty());
                }

                // CC test details
                List<Map<String, Object>> ccTestDetails = new ArrayList<>();
                for (String test : cc.getTests()) {
                    ccTestDetails.add(buildTestDetail(test));
                }
                ccMap.put("testDetails", ccTestDetails);
                ccMap.put("hasTests", !cc.getTests().isEmpty());
                ccMap.put("testCount", cc.getTests().size());

                cornerCases.add(ccMap);
            }
            reqMap.put("cornerCases", cornerCases);

            // Violations for this requirement
            List<Violation> reqViolations = violationsByReq.getOrDefault(req.getId(), Collections.emptyList());
            reqMap.put("hasViolations", !reqViolations.isEmpty());
            List<Map<String, Object>> reqViolationList = new ArrayList<>();
            for (Violation v : reqViolations) {
                Map<String, Object> vMap = new HashMap<>();
                vMap.put("severity", v.getType().getSeverity().name().toLowerCase());
                vMap.put("isError", v.getType().getSeverity() == Severity.ERROR);
                vMap.put("message", v.getMessage());
                reqViolationList.add(vMap);
            }
            reqMap.put("violations", reqViolationList);

            reqs.add(reqMap);
        }
        model.put("requirements", reqs);

        // Global violations (not tied to a specific requirement)
        model.put("hasViolations", !globalViolations.isEmpty());
        List<Map<String, Object>> violationList = new ArrayList<>();
        for (Violation v : globalViolations) {
            Map<String, Object> vMap = new HashMap<>();
            vMap.put("type", v.getType().name());
            vMap.put("severity", v.getType().getSeverity().name().toLowerCase());
            vMap.put("isError", v.getType().getSeverity() == Severity.ERROR);
            vMap.put("isWarn", v.getType().getSeverity() == Severity.WARN);
            vMap.put("message", v.getMessage());
            violationList.add(vMap);
        }
        model.put("violations", violationList);

        return model;
    }

    private Map<String, Object> buildTestDetail(String test) {
        Map<String, Object> td = new HashMap<>();
        td.put("fullName", test);
        int hash = test.indexOf('#');
        td.put("shortName", hash >= 0 ? test.substring(hash + 1) : test);
        td.put("className", hash >= 0 ? test.substring(0, hash) : test);
        return td;
    }

    private Map<String, Object> buildAggregateViewModel(AggregateReportData data) {
        Map<String, Object> model = new HashMap<>();

        model.put("generatedAt", DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                .withZone(ZoneId.systemDefault()).format(Instant.now()));

        // Aggregate summary — merged contract coverage
        CoverageSummary s = data.getAggregateSummary();
        int coveredItems = s.getCoveredRequirements() + s.getCoveredCornerCases();
        int totalItems = s.getActiveRequirements() + s.getTotalCornerCases();
        double contractCoverage = totalItems == 0 ? 0.0 : (double) coveredItems / totalItems * 100;
        model.put("contractCoverage", formatPercent(contractCoverage));
        model.put("coveredItems", coveredItems);
        model.put("totalItems", totalItems);
        model.put("coveredRequirements", s.getCoveredRequirements());
        model.put("activeRequirements", s.getActiveRequirements());
        model.put("coveredCornerCases", s.getCoveredCornerCases());
        model.put("totalCornerCases", s.getTotalCornerCases());
        model.put("moduleCount", data.getModules().size());

        // Per-module breakdown
        List<Map<String, Object>> modules = new ArrayList<>();
        for (CoverageMatrix matrix : data.getModules()) {
            Map<String, Object> moduleMap = new HashMap<>();
            moduleMap.put("moduleId", matrix.getModule().getId());
            moduleMap.put("moduleName", matrix.getModule().getName() != null
                    ? matrix.getModule().getName() : matrix.getModule().getId());
            CoverageSummary ms = matrix.getSummary();
            int mCovered = ms.getCoveredRequirements() + ms.getCoveredCornerCases();
            int mTotal = ms.getActiveRequirements() + ms.getTotalCornerCases();
            double mRate = mTotal == 0 ? 0.0 : (double) mCovered / mTotal * 100;
            moduleMap.put("contractCoverage", formatPercent(mRate));
            moduleMap.put("coveredItems", mCovered);
            moduleMap.put("totalItems", mTotal);
            moduleMap.put("violationCount", matrix.getViolations().size());

            // Requirements for this module
            List<Map<String, Object>> reqs = new ArrayList<>();
            for (RequirementCoverage req : matrix.getRequirements()) {
                Map<String, Object> reqMap = new HashMap<>();
                reqMap.put("id", req.getId());
                reqMap.put("status", req.getStatus().name().toLowerCase());
                reqMap.put("statusClass", statusClass(req.getStatus(), req.isCovered(), req.getPassing()));
                reqMap.put("covered", req.isCovered());
                reqMap.put("uncovered", !req.isCovered());
                reqMap.put("passing", req.getPassing() != null && req.getPassing());
                reqMap.put("failing", req.getPassing() != null && !req.getPassing());
                reqMap.put("testCount", req.getTests().size());
                reqMap.put("hasCornerCases", !req.getCornerCases().isEmpty());

                List<Map<String, Object>> cornerCases = new ArrayList<>();
                for (CornerCaseCoverage cc : req.getCornerCases()) {
                    Map<String, Object> ccMap = new HashMap<>();
                    ccMap.put("id", cc.getId());
                    ccMap.put("covered", cc.isCovered());
                    ccMap.put("uncovered", !cc.isCovered());
                    cornerCases.add(ccMap);
                }
                reqMap.put("cornerCases", cornerCases);
                reqs.add(reqMap);
            }
            moduleMap.put("requirements", reqs);
            moduleMap.put("hasRequirements", !reqs.isEmpty());
            modules.add(moduleMap);
        }
        model.put("modules", modules);

        // All violations
        List<Violation> allViolations = data.getAllViolations();
        model.put("hasViolations", !allViolations.isEmpty());
        List<Map<String, Object>> violationList = new ArrayList<>();
        for (Violation v : allViolations) {
            Map<String, Object> vMap = new HashMap<>();
            vMap.put("type", v.getType().name());
            vMap.put("severity", v.getType().getSeverity().name().toLowerCase());
            vMap.put("isError", v.getType().getSeverity() == Severity.ERROR);
            vMap.put("isWarn", v.getType().getSeverity() == Severity.WARN);
            vMap.put("requirementId", v.getRequirementId());
            vMap.put("cornerCaseId", v.getCornerCaseId());
            vMap.put("message", v.getMessage());
            violationList.add(vMap);
        }
        model.put("violations", violationList);

        return model;
    }

    private String statusClass(RequirementStatus status, boolean covered, Boolean passing) {
        if (status == RequirementStatus.DEPRECATED || status == RequirementStatus.SUPERSEDED) {
            return "inactive";
        }
        if (passing != null) {
            return passing ? "passing" : "failing";
        }
        return covered ? "covered" : "uncovered";
    }

    private String coverageLabel(boolean covered, Boolean passing) {
        if (passing != null) {
            return passing ? "passing" : "failing";
        }
        return covered ? "covered" : "uncovered";
    }

    private String coverageClass(boolean covered, Boolean passing) {
        if (passing != null) {
            return passing ? "passing" : "failing";
        }
        return covered ? "covered" : "uncovered";
    }

    private String formatPercent(double value) {
        return String.format("%.1f", value);
    }

}
