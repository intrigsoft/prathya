package dev.prathya.core.model;

public class Violation {

    private final ViolationType type;
    private final String requirementId;
    private final String cornerCaseId;
    private final String message;

    public Violation(ViolationType type, String requirementId, String cornerCaseId, String message) {
        this.type = type;
        this.requirementId = requirementId;
        this.cornerCaseId = cornerCaseId;
        this.message = message;
    }

    public ViolationType getType() { return type; }
    public String getRequirementId() { return requirementId; }
    public String getCornerCaseId() { return cornerCaseId; }
    public String getMessage() { return message; }
}
