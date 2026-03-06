package dev.pactum.core.model;

public class CoverageSummary {

    private final int totalRequirements;
    private final int activeRequirements;
    private final int coveredRequirements;
    private final int totalCornerCases;
    private final int coveredCornerCases;
    private final double requirementCoverage;
    private final double cornerCaseCoverage;

    public CoverageSummary(int totalRequirements, int activeRequirements, int coveredRequirements,
                           int totalCornerCases, int coveredCornerCases,
                           double requirementCoverage, double cornerCaseCoverage) {
        this.totalRequirements = totalRequirements;
        this.activeRequirements = activeRequirements;
        this.coveredRequirements = coveredRequirements;
        this.totalCornerCases = totalCornerCases;
        this.coveredCornerCases = coveredCornerCases;
        this.requirementCoverage = requirementCoverage;
        this.cornerCaseCoverage = cornerCaseCoverage;
    }

    public int getTotalRequirements() { return totalRequirements; }
    public int getActiveRequirements() { return activeRequirements; }
    public int getCoveredRequirements() { return coveredRequirements; }
    public int getTotalCornerCases() { return totalCornerCases; }
    public int getCoveredCornerCases() { return coveredCornerCases; }
    public double getRequirementCoverage() { return requirementCoverage; }
    public double getCornerCaseCoverage() { return cornerCaseCoverage; }
}
