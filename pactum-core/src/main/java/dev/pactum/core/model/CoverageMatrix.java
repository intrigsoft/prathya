package dev.pactum.core.model;

import java.util.List;

public class CoverageMatrix {

    private final ModuleInfo module;
    private final CoverageSummary summary;
    private final List<RequirementCoverage> requirements;
    private final List<Violation> violations;

    public CoverageMatrix(ModuleInfo module, CoverageSummary summary,
                          List<RequirementCoverage> requirements, List<Violation> violations) {
        this.module = module;
        this.summary = summary;
        this.requirements = requirements;
        this.violations = violations;
    }

    public ModuleInfo getModule() { return module; }
    public CoverageSummary getSummary() { return summary; }
    public List<RequirementCoverage> getRequirements() { return requirements; }
    public List<Violation> getViolations() { return violations; }
}
