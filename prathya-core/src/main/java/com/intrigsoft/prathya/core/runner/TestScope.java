package com.intrigsoft.prathya.core.runner;

public enum TestScope {
    UNIT,
    INTEGRATION,
    ALL;

    public static TestScope fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNIT;
        }
        return valueOf(value.trim().toUpperCase());
    }
}
