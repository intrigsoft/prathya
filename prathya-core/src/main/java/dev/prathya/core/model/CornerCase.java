package dev.prathya.core.model;

public class CornerCase {

    private String id;
    private String description;
    private TestEnvironment testEnvironment;

    public CornerCase() {}

    public CornerCase(String id, String description) {
        this(id, description, null);
    }

    public CornerCase(String id, String description, TestEnvironment testEnvironment) {
        this.id = id;
        this.description = description;
        this.testEnvironment = testEnvironment;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TestEnvironment getTestEnvironment() { return testEnvironment; }
    public void setTestEnvironment(TestEnvironment testEnvironment) { this.testEnvironment = testEnvironment; }
}
