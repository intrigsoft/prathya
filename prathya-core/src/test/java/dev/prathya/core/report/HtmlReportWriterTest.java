package dev.prathya.core.report;

import dev.prathya.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlReportWriterTest {

    private final HtmlReportWriter writer = new HtmlReportWriter();

    @Test
    void createsIndexHtmlInOutputDir(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        writer.writeHtmlReport(matrix, List.of(), tempDir);

        Path indexHtml = tempDir.resolve("index.html");
        assertTrue(Files.exists(indexHtml));
    }

    @Test
    void htmlContainsModuleNameAndRequirementIds(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        writer.writeHtmlReport(matrix, List.of(), tempDir);

        String html = Files.readString(tempDir.resolve("index.html"));
        assertTrue(html.contains("Authentication"));
        assertTrue(html.contains("AUTH-001"));
    }

    @Test
    void htmlContainsViolationsWhenPresent(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        Violation v = new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-002", null, "No tests for AUTH-002");
        writer.writeHtmlReport(matrix, List.of(v), tempDir);

        String html = Files.readString(tempDir.resolve("index.html"));
        assertTrue(html.contains("Violations"));
        assertTrue(html.contains("UNCOVERED_REQUIREMENT"));
        assertTrue(html.contains("No tests for AUTH-002"));
        assertTrue(html.contains("severity-error"));
        assertTrue(html.contains("error"));
    }

    @Test
    void htmlIsSelfContainedWithStyleTag(@TempDir Path tempDir) throws Exception {
        CoverageMatrix matrix = buildMatrix();
        writer.writeHtmlReport(matrix, List.of(), tempDir);

        String html = Files.readString(tempDir.resolve("index.html"));
        assertTrue(html.contains("<style>"));
        assertTrue(html.contains("</style>"));
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
