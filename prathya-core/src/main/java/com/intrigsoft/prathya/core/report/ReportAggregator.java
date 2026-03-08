package com.intrigsoft.prathya.core.report;

import com.intrigsoft.prathya.core.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportAggregator {

    private final JsonReportReader reader = new JsonReportReader();

    public AggregateReportData aggregate(List<Path> jsonReportPaths) throws IOException {
        if (jsonReportPaths.isEmpty()) {
            CoverageSummary emptySummary = new CoverageSummary(0, 0, 0, 0, 0, 0.0, 0.0);
            return new AggregateReportData(Collections.emptyList(), emptySummary, Collections.emptyList());
        }

        List<CoverageMatrix> modules = new ArrayList<>();
        for (Path path : jsonReportPaths) {
            modules.add(reader.read(path));
        }

        int totalRequirements = 0;
        int activeRequirements = 0;
        int coveredRequirements = 0;
        int totalCornerCases = 0;
        int coveredCornerCases = 0;

        List<Violation> allViolations = new ArrayList<>();

        for (CoverageMatrix matrix : modules) {
            CoverageSummary s = matrix.getSummary();
            totalRequirements += s.getTotalRequirements();
            activeRequirements += s.getActiveRequirements();
            coveredRequirements += s.getCoveredRequirements();
            totalCornerCases += s.getTotalCornerCases();
            coveredCornerCases += s.getCoveredCornerCases();

            String moduleId = matrix.getModule().getId();
            for (Violation v : matrix.getViolations()) {
                allViolations.add(new Violation(
                        v.getType(), v.getRequirementId(), v.getCornerCaseId(),
                        "[" + moduleId + "] " + v.getMessage()));
            }
        }

        double requirementCoverage = activeRequirements > 0
                ? (coveredRequirements * 100.0 / activeRequirements) : 0.0;
        double cornerCaseCoverage = totalCornerCases > 0
                ? (coveredCornerCases * 100.0 / totalCornerCases) : 0.0;

        CoverageSummary aggregateSummary = new CoverageSummary(
                totalRequirements, activeRequirements, coveredRequirements,
                totalCornerCases, coveredCornerCases,
                requirementCoverage, cornerCaseCoverage);

        return new AggregateReportData(modules, aggregateSummary, allViolations);
    }
}
