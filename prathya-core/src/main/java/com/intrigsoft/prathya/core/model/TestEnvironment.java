package com.intrigsoft.prathya.core.model;

public enum TestEnvironment {
    UNIT("unit"),
    INTEGRATION("integration"),
    FULL_SERVER("full-server");

    private final String yamlValue;

    TestEnvironment(String yamlValue) {
        this.yamlValue = yamlValue;
    }

    public String toYaml() {
        return yamlValue;
    }

    public static TestEnvironment fromYaml(String value) {
        if (value == null) return null;
        for (TestEnvironment env : values()) {
            if (env.yamlValue.equals(value)) {
                return env;
            }
        }
        throw new IllegalArgumentException("Unknown test_environment: '" + value
                + "'. Valid values: unit, integration, full-server");
    }
}
