package dev.prathya.core.model;

public enum ViolationType {
    ORPHANED_ANNOTATION(Severity.ERROR),
    UNCOVERED_REQUIREMENT(Severity.ERROR),
    UNCOVERED_CORNER_CASE(Severity.WARN),
    DEPRECATED_REFERENCE(Severity.WARN),
    SUPERSEDED_REFERENCE(Severity.WARN),
    UNCOVERED_CORNER_CASE_ENVIRONMENT(Severity.INFO),
    COVERAGE_BELOW_THRESHOLD(Severity.ERROR);

    private final Severity severity;

    ViolationType(Severity severity) {
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }
}
