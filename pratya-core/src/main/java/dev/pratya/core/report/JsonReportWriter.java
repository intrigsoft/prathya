package dev.pratya.core.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pratya.core.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public class JsonReportWriter implements ReportWriter {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void writeJsonReport(CoverageMatrix matrix, List<Violation> violations, Path outputFile) throws IOException {
        ObjectNode root = mapper.createObjectNode();

        root.put("module", matrix.getModule().getId());
        root.put("generatedAt", Instant.now().toString());

        writeSummaryNode(root.putObject("summary"), matrix.getSummary());
        writeRequirementsArray(root.putArray("requirements"), matrix.getRequirements());
        writeViolationsArray(root.putArray("violations"), violations);

        Files.createDirectories(outputFile.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), root);
    }

    public void writeAggregateJsonReport(AggregateReportData data, Path outputFile) throws IOException {
        ObjectNode root = mapper.createObjectNode();

        root.put("type", "aggregate");
        root.put("generatedAt", Instant.now().toString());

        // Aggregate summary
        writeSummaryNode(root.putObject("summary"), data.getAggregateSummary());

        // Per-module entries
        ArrayNode modulesArray = root.putArray("modules");
        for (CoverageMatrix matrix : data.getModules()) {
            ObjectNode moduleNode = modulesArray.addObject();
            moduleNode.put("module", matrix.getModule().getId());
            writeSummaryNode(moduleNode.putObject("summary"), matrix.getSummary());
            writeRequirementsArray(moduleNode.putArray("requirements"), matrix.getRequirements());
            writeViolationsArray(moduleNode.putArray("violations"), matrix.getViolations());
        }

        // All violations (with module prefix in message)
        writeViolationsArray(root.putArray("violations"), data.getAllViolations());

        Files.createDirectories(outputFile.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), root);
    }

    private void writeSummaryNode(ObjectNode summary, CoverageSummary s) {
        summary.put("totalRequirements", s.getTotalRequirements());
        summary.put("activeRequirements", s.getActiveRequirements());
        summary.put("coveredRequirements", s.getCoveredRequirements());
        summary.put("requirementCoverage", s.getRequirementCoverage());
        summary.put("totalCornerCases", s.getTotalCornerCases());
        summary.put("coveredCornerCases", s.getCoveredCornerCases());
        summary.put("cornerCaseCoverage", s.getCornerCaseCoverage());
    }

    private void writeRequirementsArray(ArrayNode requirements, List<RequirementCoverage> reqs) {
        for (RequirementCoverage req : reqs) {
            ObjectNode reqNode = requirements.addObject();
            reqNode.put("id", req.getId());
            reqNode.put("status", req.getStatus().name().toLowerCase());
            reqNode.put("covered", req.isCovered());

            ArrayNode tests = reqNode.putArray("tests");
            for (String test : req.getTests()) {
                tests.add(test);
            }

            if (req.getPassing() == null) {
                reqNode.putNull("passing");
            } else {
                reqNode.put("passing", req.getPassing());
            }

            ArrayNode cornerCases = reqNode.putArray("cornerCases");
            for (CornerCaseCoverage cc : req.getCornerCases()) {
                ObjectNode ccNode = cornerCases.addObject();
                ccNode.put("id", cc.getId());
                ccNode.put("covered", cc.isCovered());
                if (cc.getPassing() == null) {
                    ccNode.putNull("passing");
                } else {
                    ccNode.put("passing", cc.getPassing());
                }
            }
        }
    }

    private void writeViolationsArray(ArrayNode violationsArray, List<Violation> violations) {
        for (Violation v : violations) {
            ObjectNode vNode = violationsArray.addObject();
            vNode.put("type", v.getType().name());
            vNode.put("severity", v.getType().getSeverity().name().toLowerCase());
            vNode.put("requirementId", v.getRequirementId());
            if (v.getCornerCaseId() == null) {
                vNode.putNull("cornerCaseId");
            } else {
                vNode.put("cornerCaseId", v.getCornerCaseId());
            }
            vNode.put("message", v.getMessage());
        }
    }

    @Override
    public void writeHtmlReport(CoverageMatrix matrix, List<Violation> violations, Path outputDir) throws IOException {
        throw new UnsupportedOperationException("JsonReportWriter does not support HTML output");
    }
}
