package dev.pratya.core.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pratya.core.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonReportReader {

    private final ObjectMapper mapper = new ObjectMapper();

    public CoverageMatrix read(Path jsonFile) throws IOException {
        JsonNode root = mapper.readTree(jsonFile.toFile());

        ModuleInfo module = readModule(root);
        CoverageSummary summary = readSummary(root.get("summary"));
        List<RequirementCoverage> requirements = readRequirements(root.get("requirements"));
        List<Violation> violations = readViolations(root.get("violations"));

        return new CoverageMatrix(module, summary, requirements, violations);
    }

    private ModuleInfo readModule(JsonNode root) {
        ModuleInfo module = new ModuleInfo();
        module.setId(root.get("module").asText());
        return module;
    }

    private CoverageSummary readSummary(JsonNode node) {
        return new CoverageSummary(
                node.get("totalRequirements").asInt(),
                node.get("activeRequirements").asInt(),
                node.get("coveredRequirements").asInt(),
                node.get("totalCornerCases").asInt(),
                node.get("coveredCornerCases").asInt(),
                node.get("requirementCoverage").asDouble(),
                node.get("cornerCaseCoverage").asDouble()
        );
    }

    private List<RequirementCoverage> readRequirements(JsonNode array) {
        List<RequirementCoverage> requirements = new ArrayList<>();
        if (array == null || !array.isArray()) return requirements;

        for (JsonNode reqNode : array) {
            String id = reqNode.get("id").asText();
            RequirementStatus status = RequirementStatus.valueOf(reqNode.get("status").asText().toUpperCase());
            boolean covered = reqNode.get("covered").asBoolean();

            List<String> tests = new ArrayList<>();
            for (JsonNode t : reqNode.get("tests")) {
                tests.add(t.asText());
            }

            Boolean passing = reqNode.get("passing").isNull() ? null : reqNode.get("passing").asBoolean();

            List<CornerCaseCoverage> cornerCases = new ArrayList<>();
            for (JsonNode ccNode : reqNode.get("cornerCases")) {
                String ccId = ccNode.get("id").asText();
                boolean ccCovered = ccNode.get("covered").asBoolean();
                Boolean ccPassing = ccNode.get("passing").isNull() ? null : ccNode.get("passing").asBoolean();
                cornerCases.add(new CornerCaseCoverage(ccId, ccCovered, ccPassing));
            }

            requirements.add(new RequirementCoverage(id, status, covered, tests, cornerCases, passing));
        }
        return requirements;
    }

    private List<Violation> readViolations(JsonNode array) {
        List<Violation> violations = new ArrayList<>();
        if (array == null || !array.isArray()) return violations;

        for (JsonNode vNode : array) {
            ViolationType type = ViolationType.valueOf(vNode.get("type").asText());
            String requirementId = vNode.has("requirementId") && !vNode.get("requirementId").isNull()
                    ? vNode.get("requirementId").asText() : null;
            String cornerCaseId = vNode.has("cornerCaseId") && !vNode.get("cornerCaseId").isNull()
                    ? vNode.get("cornerCaseId").asText() : null;
            String message = vNode.get("message").asText();
            violations.add(new Violation(type, requirementId, cornerCaseId, message));
        }
        return violations;
    }
}
