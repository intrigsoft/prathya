package com.intrigsoft.prathya.core.model;

import java.util.List;

public class AggregateReportData {

    private final List<CoverageMatrix> modules;
    private final CoverageSummary aggregateSummary;
    private final List<Violation> allViolations;

    public AggregateReportData(List<CoverageMatrix> modules, CoverageSummary aggregateSummary,
                               List<Violation> allViolations) {
        this.modules = modules;
        this.aggregateSummary = aggregateSummary;
        this.allViolations = allViolations;
    }

    public List<CoverageMatrix> getModules() { return modules; }
    public CoverageSummary getAggregateSummary() { return aggregateSummary; }
    public List<Violation> getAllViolations() { return allViolations; }
}
