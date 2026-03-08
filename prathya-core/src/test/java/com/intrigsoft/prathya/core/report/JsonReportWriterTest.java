package com.intrigsoft.prathya.core.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrigsoft.prathya.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportWriterTest {

    private final JsonReportWriter writer = new JsonReportWriter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesValidJsonWithAllFields(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        List<Violation> violations = List.of(
                new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-002", null, "No tests for AUTH-002"));

        Path output = tempDir.resolve("report.json");
        writer.writeJsonReport(matrix, violations, output);

        assertTrue(Files.exists(output));
        JsonNode root = mapper.readTree(output.toFile());

        assertEquals("AUTH", root.get("module").asText());
        assertNotNull(root.get("generatedAt"));
        assertNotNull(root.get("summary"));
        assertTrue(root.get("requirements").isArray());
        assertTrue(root.get("violations").isArray());
    }

    @Test
    void statusSerializedAsLowercase(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        Path output = tempDir.resolve("report.json");
        writer.writeJsonReport(matrix, List.of(), output);

        JsonNode root = mapper.readTree(output.toFile());
        JsonNode firstReq = root.get("requirements").get(0);
        assertEquals("approved", firstReq.get("status").asText());
    }

    @Test
    void passingNullSerializedAsJsonNull(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        Path output = tempDir.resolve("report.json");
        writer.writeJsonReport(matrix, List.of(), output);

        JsonNode root = mapper.readTree(output.toFile());
        JsonNode firstCc = root.get("requirements").get(0).get("cornerCases").get(0);
        assertTrue(firstCc.get("passing").isNull());
    }

    @Test
    void violationsIncludedInOutput(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        Violation v = new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-002", null, "No tests");
        Path output = tempDir.resolve("report.json");
        writer.writeJsonReport(matrix, List.of(v), output);

        JsonNode root = mapper.readTree(output.toFile());
        JsonNode violations = root.get("violations");
        assertEquals(1, violations.size());
        assertEquals("UNCOVERED_REQUIREMENT", violations.get(0).get("type").asText());
        assertEquals("error", violations.get(0).get("severity").asText());
        assertEquals("AUTH-002", violations.get(0).get("requirementId").asText());
        assertTrue(violations.get(0).get("cornerCaseId").isNull());
    }

    @Test
    void generatedAtIsValidIsoInstant(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        Path output = tempDir.resolve("report.json");
        writer.writeJsonReport(matrix, List.of(), output);

        JsonNode root = mapper.readTree(output.toFile());
        String generatedAt = root.get("generatedAt").asText();
        assertDoesNotThrow(() -> Instant.parse(generatedAt));
    }

    private CoverageMatrix buildMatrix() {
        ModuleInfo module = new ModuleInfo();
        module.setId("AUTH");
        module.setName("Authentication");

        CornerCaseCoverage cc = new CornerCaseCoverage("AUTH-001-CC-001", true, null);
        RequirementCoverage req = new RequirementCoverage(
                "AUTH-001", RequirementStatus.APPROVED, true,
                List.of("AuthTest#loginWithValidCredentials"), List.of(cc));

        CoverageSummary summary = new CoverageSummary(1, 1, 1, 1, 1, 100.0, 100.0);
        return new CoverageMatrix(module, summary, List.of(req), List.of());
    }
}
