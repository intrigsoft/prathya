package dev.pactum.core.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pactum.core.model.*;

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

        // Summary
        ObjectNode summary = root.putObject("summary");
        summary.put("totalRequirements", matrix.getSummary().getTotalRequirements());
        summary.put("activeRequirements", matrix.getSummary().getActiveRequirements());
        summary.put("coveredRequirements", matrix.getSummary().getCoveredRequirements());
        summary.put("requirementCoverage", matrix.getSummary().getRequirementCoverage());
        summary.put("totalCornerCases", matrix.getSummary().getTotalCornerCases());
        summary.put("coveredCornerCases", matrix.getSummary().getCoveredCornerCases());
        summary.put("cornerCaseCoverage", matrix.getSummary().getCornerCaseCoverage());

        // Requirements
        ArrayNode requirements = root.putArray("requirements");
        for (RequirementCoverage req : matrix.getRequirements()) {
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

        // Violations
        ArrayNode violationsArray = root.putArray("violations");
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

        Files.createDirectories(outputFile.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), root);
    }

    @Override
    public void writeHtmlReport(CoverageMatrix matrix, List<Violation> violations, Path outputDir) throws IOException {
        throw new UnsupportedOperationException("JsonReportWriter does not support HTML output");
    }
}
