package dev.prathya.core.model;

import java.util.List;

public class CoverageMatrix {

    private final ModuleInfo module;
    private final CoverageSummary summary;
    private final List<RequirementCoverage> requirements;
    private final List<Violation> violations;
    private final ModuleContract contract; // optional, may be null
    private final CodeCoverageSummary codeCoverage; // optional, may be null
    private final CodeCoverageSummary contractCodeCoverage; // optional, may be null — only from prathya:run

    public CoverageMatrix(ModuleInfo module, CoverageSummary summary,
                          List<RequirementCoverage> requirements, List<Violation> violations) {
        this(module, summary, requirements, violations, null, null, null);
    }

    public CoverageMatrix(ModuleInfo module, CoverageSummary summary,
                          List<RequirementCoverage> requirements, List<Violation> violations,
                          ModuleContract contract) {
        this(module, summary, requirements, violations, contract, null, null);
    }

    public CoverageMatrix(ModuleInfo module, CoverageSummary summary,
                          List<RequirementCoverage> requirements, List<Violation> violations,
                          ModuleContract contract, CodeCoverageSummary codeCoverage) {
        this(module, summary, requirements, violations, contract, codeCoverage, null);
    }

    public CoverageMatrix(ModuleInfo module, CoverageSummary summary,
                          List<RequirementCoverage> requirements, List<Violation> violations,
                          ModuleContract contract, CodeCoverageSummary codeCoverage,
                          CodeCoverageSummary contractCodeCoverage) {
        this.module = module;
        this.summary = summary;
        this.requirements = requirements;
        this.violations = violations;
        this.contract = contract;
        this.codeCoverage = codeCoverage;
        this.contractCodeCoverage = contractCodeCoverage;
    }

    public ModuleInfo getModule() { return module; }
    public CoverageSummary getSummary() { return summary; }
    public List<RequirementCoverage> getRequirements() { return requirements; }
    public List<Violation> getViolations() { return violations; }
    public ModuleContract getContract() { return contract; }
    public CodeCoverageSummary getCodeCoverage() { return codeCoverage; }
    public CodeCoverageSummary getContractCodeCoverage() { return contractCodeCoverage; }
}
