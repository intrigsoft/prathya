package com.intrigsoft.prathya.core.model;

import java.util.List;

public class TraceEntry {

    private final String className;
    private final String methodName;
    private final List<String> requirementIds;

    public TraceEntry(String className, String methodName, List<String> requirementIds) {
        this.className = className;
        this.methodName = methodName;
        this.requirementIds = requirementIds;
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public List<String> getRequirementIds() { return requirementIds; }
}
