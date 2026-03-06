package dev.pratya.core.report;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import dev.pratya.core.model.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlReportWriter implements ReportWriter {

    private static final String TEMPLATE_PATH = "/templates/pratya-report.mustache";
    private static final String AGGREGATE_TEMPLATE_PATH = "/templates/pratya-aggregate-report.mustache";

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
        model.put("generatedAt", Instant.now().toString());

        // Summary
        model.put("totalRequirements", matrix.getSummary().getTotalRequirements());
        model.put("activeRequirements", matrix.getSummary().getActiveRequirements());
        model.put("coveredRequirements", matrix.getSummary().getCoveredRequirements());
        model.put("requirementCoverage", formatPercent(matrix.getSummary().getRequirementCoverage()));
        model.put("totalCornerCases", matrix.getSummary().getTotalCornerCases());
        model.put("coveredCornerCases", matrix.getSummary().getCoveredCornerCases());
        model.put("cornerCaseCoverage", formatPercent(matrix.getSummary().getCornerCaseCoverage()));

        // Requirements
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
            reqMap.put("tests", req.getTests());
            reqMap.put("hasCornerCases", !req.getCornerCases().isEmpty());

            List<Map<String, Object>> cornerCases = new ArrayList<>();
            for (CornerCaseCoverage cc : req.getCornerCases()) {
                Map<String, Object> ccMap = new HashMap<>();
                ccMap.put("id", cc.getId());
                ccMap.put("covered", cc.isCovered());
                ccMap.put("uncovered", !cc.isCovered());
                ccMap.put("passing", cc.getPassing());
                cornerCases.add(ccMap);
            }
            reqMap.put("cornerCases", cornerCases);

            reqs.add(reqMap);
        }
        model.put("requirements", reqs);

        // Violations
        model.put("hasViolations", !violations.isEmpty());
        List<Map<String, Object>> violationList = new ArrayList<>();
        for (Violation v : violations) {
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

    private Map<String, Object> buildAggregateViewModel(AggregateReportData data) {
        Map<String, Object> model = new HashMap<>();

        model.put("generatedAt", Instant.now().toString());

        // Aggregate summary
        CoverageSummary s = data.getAggregateSummary();
        model.put("totalRequirements", s.getTotalRequirements());
        model.put("activeRequirements", s.getActiveRequirements());
        model.put("coveredRequirements", s.getCoveredRequirements());
        model.put("requirementCoverage", formatPercent(s.getRequirementCoverage()));
        model.put("totalCornerCases", s.getTotalCornerCases());
        model.put("coveredCornerCases", s.getCoveredCornerCases());
        model.put("cornerCaseCoverage", formatPercent(s.getCornerCaseCoverage()));
        model.put("moduleCount", data.getModules().size());

        // Per-module breakdown
        List<Map<String, Object>> modules = new ArrayList<>();
        for (CoverageMatrix matrix : data.getModules()) {
            Map<String, Object> moduleMap = new HashMap<>();
            moduleMap.put("moduleId", matrix.getModule().getId());
            moduleMap.put("moduleName", matrix.getModule().getName() != null
                    ? matrix.getModule().getName() : matrix.getModule().getId());
            moduleMap.put("requirementCoverage", formatPercent(matrix.getSummary().getRequirementCoverage()));
            moduleMap.put("coveredRequirements", matrix.getSummary().getCoveredRequirements());
            moduleMap.put("activeRequirements", matrix.getSummary().getActiveRequirements());
            moduleMap.put("cornerCaseCoverage", formatPercent(matrix.getSummary().getCornerCaseCoverage()));
            moduleMap.put("coveredCornerCases", matrix.getSummary().getCoveredCornerCases());
            moduleMap.put("totalCornerCases", matrix.getSummary().getTotalCornerCases());
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

    private String formatPercent(double value) {
        return String.format("%.1f", value);
    }
}
