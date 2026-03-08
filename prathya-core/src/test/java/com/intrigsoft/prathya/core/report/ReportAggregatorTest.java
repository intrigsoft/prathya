package com.intrigsoft.prathya.core.report;

import com.intrigsoft.prathya.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportAggregatorTest {

    private final JsonReportWriter writer = new JsonReportWriter();
    private final ReportAggregator aggregator = new ReportAggregator();

    @Test
    void aggregate_twoModules(@TempDir Path tempDir) throws Exception {
        Path report1 = writeReport(tempDir, "AUTH", 4, 4, 3, 2, 1, 75.0, 50.0,
                List.of(new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-004", null, "Not covered")));
        Path report2 = writeReport(tempDir, "BILLING", 3, 3, 2, 4, 3, 66.7, 75.0,
                List.of(new Violation(ViolationType.UNCOVERED_CORNER_CASE, "BILL-001", "BILL-001-CC-02", "Missing")));

        AggregateReportData result = aggregator.aggregate(List.of(report1, report2));

        assertEquals(2, result.getModules().size());

        CoverageSummary summary = result.getAggregateSummary();
        assertEquals(7, summary.getTotalRequirements());
        assertEquals(7, summary.getActiveRequirements());
        assertEquals(5, summary.getCoveredRequirements());
        assertEquals(6, summary.getTotalCornerCases());
        assertEquals(4, summary.getCoveredCornerCases());

        // 5/7 = 71.4%
        assertEquals(71.4, summary.getRequirementCoverage(), 0.1);
        // 4/6 = 66.7%
        assertEquals(66.7, summary.getCornerCaseCoverage(), 0.1);
    }

    @Test
    void aggregate_singleModule(@TempDir Path tempDir) throws Exception {
        Path report = writeReport(tempDir, "AUTH", 3, 3, 2, 1, 1, 66.7, 100.0, List.of());

        AggregateReportData result = aggregator.aggregate(List.of(report));

        assertEquals(1, result.getModules().size());
        assertEquals(3, result.getAggregateSummary().getTotalRequirements());
        assertEquals(2, result.getAggregateSummary().getCoveredRequirements());
        assertEquals(66.7, result.getAggregateSummary().getRequirementCoverage(), 0.1);
    }

    @Test
    void aggregate_emptyList() throws Exception {
        AggregateReportData result = aggregator.aggregate(List.of());

        assertTrue(result.getModules().isEmpty());
        assertEquals(0, result.getAggregateSummary().getTotalRequirements());
        assertEquals(0, result.getAggregateSummary().getActiveRequirements());
        assertEquals(0.0, result.getAggregateSummary().getRequirementCoverage());
        assertTrue(result.getAllViolations().isEmpty());
    }

    @Test
    void aggregate_violationsCollected(@TempDir Path tempDir) throws Exception {
        Violation v1 = new Violation(ViolationType.UNCOVERED_REQUIREMENT, "AUTH-002", null, "Not covered");
        Violation v2 = new Violation(ViolationType.UNCOVERED_CORNER_CASE, "BILL-001", "BILL-001-CC-01", "CC missing");
        Violation v3 = new Violation(ViolationType.ORPHANED_ANNOTATION, "PAY-099", null, "No requirement");

        Path report1 = writeReport(tempDir, "AUTH", 2, 2, 1, 0, 0, 50.0, 0.0, List.of(v1));
        Path report2 = writeReport(tempDir, "BILLING", 1, 1, 1, 1, 0, 100.0, 0.0, List.of(v2, v3));

        AggregateReportData result = aggregator.aggregate(List.of(report1, report2));

        assertEquals(3, result.getAllViolations().size());
        assertTrue(result.getAllViolations().get(0).getMessage().startsWith("[AUTH]"));
        assertTrue(result.getAllViolations().get(1).getMessage().startsWith("[BILLING]"));
        assertTrue(result.getAllViolations().get(2).getMessage().startsWith("[BILLING]"));
    }

    private Path writeReport(Path tempDir, String moduleId, int total, int active, int covered,
                             int totalCc, int coveredCc, double reqCov, double ccCov,
                             List<Violation> violations) throws Exception {
        ModuleInfo module = new ModuleInfo();
        module.setId(moduleId);

        CoverageSummary summary = new CoverageSummary(total, active, covered, totalCc, coveredCc, reqCov, ccCov);
        CoverageMatrix matrix = new CoverageMatrix(module, summary, List.of(), violations);

        Path file = tempDir.resolve(moduleId + "-report.json");
        writer.writeJsonReport(matrix, violations, file);
        return file;
    }
}
