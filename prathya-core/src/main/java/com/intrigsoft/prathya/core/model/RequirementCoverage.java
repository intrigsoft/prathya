package com.intrigsoft.prathya.core.model;

import java.util.List;

public class RequirementCoverage {

    private final String id;
    private final RequirementStatus status;
    private final boolean covered;
    private final List<String> tests;
    private final List<CornerCaseCoverage> cornerCases;
    private final Boolean passing; // null = unknown, true = all pass, false = any fail

    public RequirementCoverage(String id, RequirementStatus status, boolean covered,
                               List<String> tests, List<CornerCaseCoverage> cornerCases) {
        this(id, status, covered, tests, cornerCases, null);
    }

    public RequirementCoverage(String id, RequirementStatus status, boolean covered,
                               List<String> tests, List<CornerCaseCoverage> cornerCases,
                               Boolean passing) {
        this.id = id;
        this.status = status;
        this.covered = covered;
        this.tests = tests;
        this.cornerCases = cornerCases;
        this.passing = passing;
    }

    public String getId() { return id; }
    public RequirementStatus getStatus() { return status; }
    public boolean isCovered() { return covered; }
    public List<String> getTests() { return tests; }
    public List<CornerCaseCoverage> getCornerCases() { return cornerCases; }
    public Boolean getPassing() { return passing; }
}
