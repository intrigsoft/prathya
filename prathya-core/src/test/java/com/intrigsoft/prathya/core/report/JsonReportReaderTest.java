package com.intrigsoft.prathya.core.report;

import com.intrigsoft.prathya.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportReaderTest {

    private final JsonReportWriter writer = new JsonReportWriter();
    private final JsonReportReader reader = new JsonReportReader();

    @Test
    void read_validReport(@TempDir Path tempDir) throws Exception {
        CoverageMatrix original = buildMatrix("AUTH", "Authentication");
        Path file = tempDir.resolve("report.json");
        writer.writeJsonReport(original, original.getViolations(), file);

        CoverageMatrix result = reader.read(file);

        assertEquals("AUTH", result.getModule().getId());
        assertEquals(2, result.getSummary().getTotalRequirements());
        assertEquals(2, result.getSummary().getActiveRequirements());
        assertEquals(1, result.getSummary().getCoveredRequirements());
        assertEquals(1, result.getRequirements().size());
        assertEquals("AUTH-001", result.getRequirements().get(0).getId());
        assertEquals(RequirementStatus.APPROVED, result.getRequirements().get(0).getStatus());
        assertTrue(result.getRequirements().get(0).isCovered());
    }

    @Test
    void read_roundTrip(@TempDir Path tempDir) throws Exception {
        CoverageMatrix original = buildMatrix("AUTH", "Authentication");
        List<Violation> violations = List.of(
                new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-002", null, "No tests"));
        Path file = tempDir.resolve("report.json");
        writer.writeJsonReport(original, violations, file);

        CoverageMatrix result = reader.read(file);

        assertEquals(original.getModule().getId(), result.getModule().getId());
        assertEquals(original.getSummary().getTotalRequirements(), result.getSummary().getTotalRequirements());
        assertEquals(original.getSummary().getRequirementCoverage(), result.getSummary().getRequirementCoverage());
        assertEquals(original.getRequirements().size(), result.getRequirements().size());

        RequirementCoverage origReq = original.getRequirements().get(0);
        RequirementCoverage readReq = result.getRequirements().get(0);
        assertEquals(origReq.getId(), readReq.getId());
        assertEquals(origReq.isCovered(), readReq.isCovered());
        assertEquals(origReq.getTests(), readReq.getTests());
        assertNull(readReq.getPassing());

        assertEquals(1, result.getViolations().size());
        assertEquals(ViolationType.UNCOVERED_REQUIREMENT, result.getViolations().get(0).getType());
    }

    @Test
    void read_violationsWithSeverity(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix("AUTH", "Authentication");
        List<Violation> violations = List.of(
                new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-002", null, "No tests"),
                new Violation(ViolationType.UNCOVERED_CORNER_CASE, "AUTH-001", "AUTH-001-CC-002", "Corner case missing"));
        Path file = tempDir.resolve("report.json");
        writer.writeJsonReport(matrix, violations, file);

        CoverageMatrix result = reader.read(file);

        assertEquals(2, result.getViolations().size());
        assertEquals(Severity.ERROR, result.getViolations().get(0).getType().getSeverity());
        assertEquals(Severity.WARN, result.getViolations().get(1).getType().getSeverity());
        assertEquals("AUTH-001-CC-002", result.getViolations().get(1).getCornerCaseId());
    }

    @Test
    void read_cornerCasesPreserved(@TempDir Path tempDir) throws Exception {
        CoverageMatrix original = buildMatrix("AUTH", "Authentication");
        Path file = tempDir.resolve("report.json");
        writer.writeJsonReport(original, List.of(), file);

        CoverageMatrix result = reader.read(file);

        RequirementCoverage req = result.getRequirements().get(0);
        assertEquals(1, req.getCornerCases().size());
        assertEquals("AUTH-001-CC-001", req.getCornerCases().get(0).getId());
        assertTrue(req.getCornerCases().get(0).isCovered());
        assertNull(req.getCornerCases().get(0).getPassing());
    }

    private CoverageMatrix buildMatrix(String id, String name) {
        ModuleInfo module = new ModuleInfo();
        module.setId(id);
        module.setName(name);

        CornerCaseCoverage cc = new CornerCaseCoverage("AUTH-001-CC-001", true, null);
        RequirementCoverage req = new RequirementCoverage(
                "AUTH-001", RequirementStatus.APPROVED, true,
                List.of("AuthTest#loginWithValidCredentials"), List.of(cc));

        CoverageSummary summary = new CoverageSummary(2, 2, 1, 1, 1, 50.0, 100.0);
        return new CoverageMatrix(module, summary, List.of(req), List.of());
    }
}
