package com.intrigsoft.prathya.core.model;

/**
 * Represents a production class or method marked with {@code @NonContractual}.
 * These entries are excluded from JaCoCo code coverage computation.
 */
public class NonContractualEntry {

    private final String className;
    private final String methodName;
    private final String reason;

    public NonContractualEntry(String className, String methodName, String reason) {
        this.className = className;
        this.methodName = methodName;
        this.reason = reason;
    }

    /** Fully qualified class name. */
    public String getClassName() { return className; }

    /** Method name, or {@code null} if this is a class-level exclusion. */
    public String getMethodName() { return methodName; }

    /** The reason this code is excluded from contract coverage. */
    public String getReason() { return reason; }
}
