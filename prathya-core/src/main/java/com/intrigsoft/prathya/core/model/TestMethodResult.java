package com.intrigsoft.prathya.core.model;

public class TestMethodResult {

    public enum TestOutcome {
        PASSED, FAILED, ERROR, SKIPPED
    }

    private final String className;
    private final String methodName;
    private final TestOutcome outcome;
    private final String failureMessage;

    public TestMethodResult(String className, String methodName, TestOutcome outcome, String failureMessage) {
        this.className = className;
        this.methodName = methodName;
        this.outcome = outcome;
        this.failureMessage = failureMessage;
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public TestOutcome getOutcome() { return outcome; }
    public String getFailureMessage() { return failureMessage; }
}
