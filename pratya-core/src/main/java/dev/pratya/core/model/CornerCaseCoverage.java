package dev.pratya.core.model;

public class CornerCaseCoverage {

    private final String id;
    private final boolean covered;
    private final Boolean passing; // null = unknown in v0.1

    public CornerCaseCoverage(String id, boolean covered, Boolean passing) {
        this.id = id;
        this.covered = covered;
        this.passing = passing;
    }

    public String getId() { return id; }
    public boolean isCovered() { return covered; }
    public Boolean getPassing() { return passing; }
}
