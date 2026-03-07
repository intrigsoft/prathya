package dev.prathya.core.model;

import java.util.Collections;
import java.util.List;

public class CornerCaseCoverage {

    private final String id;
    private final boolean covered;
    private final Boolean passing; // null = unknown in v0.1
    private final List<String> tests;
    private final String testEnvironment;

    public CornerCaseCoverage(String id, boolean covered, Boolean passing) {
        this(id, covered, passing, Collections.emptyList(), null);
    }

    public CornerCaseCoverage(String id, boolean covered, Boolean passing, List<String> tests) {
        this(id, covered, passing, tests, null);
    }

    public CornerCaseCoverage(String id, boolean covered, Boolean passing, List<String> tests, String testEnvironment) {
        this.id = id;
        this.covered = covered;
        this.passing = passing;
        this.tests = tests != null ? tests : Collections.emptyList();
        this.testEnvironment = testEnvironment;
    }

    public String getId() { return id; }
    public boolean isCovered() { return covered; }
    public Boolean getPassing() { return passing; }
    public List<String> getTests() { return tests; }
    public String getTestEnvironment() { return testEnvironment; }
}
