package dev.pactum.core.coverage;

import dev.pactum.core.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-references trace entries against the module contract to compute coverage.
 * Excludes deprecated/superseded requirements from coverage percentages.
 */
public class DefaultCoverageComputer implements CoverageComputer {

    @Override
    public CoverageMatrix compute(ModuleContract contract, List<TraceEntry> traces) {
        // Build a map: requirementOrCornerCaseId -> list of test labels
        Map<String, List<String>> idToTests = new HashMap<>();
        for (TraceEntry trace : traces) {
            String testLabel = trace.getClassName() + "#" + trace.getMethodName();
            for (String reqId : trace.getRequirementIds()) {
                idToTests.computeIfAbsent(reqId, k -> new ArrayList<>()).add(testLabel);
            }
        }

        List<RequirementCoverage> reqCoverages = new ArrayList<>();
        int totalRequirements = contract.getRequirements().size();
        int activeRequirements = 0;
        int coveredRequirements = 0;
        int totalCornerCases = 0;
        int coveredCornerCases = 0;

        for (RequirementDefinition req : contract.getRequirements()) {
            boolean isActive = req.getStatus() == RequirementStatus.APPROVED
                    || req.getStatus() == RequirementStatus.DRAFT;

            List<String> tests = idToTests.getOrDefault(req.getId(), Collections.emptyList());
            boolean covered = !tests.isEmpty();

            // Corner case coverage
            List<CornerCaseCoverage> ccCoverages = new ArrayList<>();
            for (CornerCase cc : req.getCornerCases()) {
                List<String> ccTests = idToTests.getOrDefault(cc.getId(), Collections.emptyList());
                boolean ccCovered = !ccTests.isEmpty();
                ccCoverages.add(new CornerCaseCoverage(cc.getId(), ccCovered, null));

                if (isActive) {
                    totalCornerCases++;
                    if (ccCovered) coveredCornerCases++;
                }
            }

            reqCoverages.add(new RequirementCoverage(
                    req.getId(), req.getStatus(), covered, tests, ccCoverages));

            if (isActive) {
                activeRequirements++;
                if (covered) coveredRequirements++;
            }
        }

        double reqCovPct = activeRequirements > 0
                ? (coveredRequirements * 100.0) / activeRequirements : 0.0;
        double ccCovPct = totalCornerCases > 0
                ? (coveredCornerCases * 100.0) / totalCornerCases : 0.0;

        CoverageSummary summary = new CoverageSummary(
                totalRequirements, activeRequirements, coveredRequirements,
                totalCornerCases, coveredCornerCases,
                reqCovPct, ccCovPct);

        return new CoverageMatrix(contract.getModule(), summary, reqCoverages, Collections.emptyList());
    }
}
