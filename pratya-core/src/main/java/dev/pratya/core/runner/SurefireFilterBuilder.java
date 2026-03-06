package dev.pratya.core.runner;

import dev.pratya.core.model.TestMethod;

import java.util.List;
import java.util.stream.Collectors;

public final class SurefireFilterBuilder {

    private SurefireFilterBuilder() {}

    public static String build(List<TestMethod> tests) {
        return tests.stream()
                .map(TestMethod::toSurefireFilter)
                .distinct()
                .collect(Collectors.joining("+"));
    }
}
