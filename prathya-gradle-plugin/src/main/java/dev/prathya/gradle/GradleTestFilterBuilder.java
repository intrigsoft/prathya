package dev.prathya.gradle;

import dev.prathya.core.model.TestMethod;

import java.util.List;
import java.util.stream.Collectors;

public final class GradleTestFilterBuilder {

    private GradleTestFilterBuilder() {}

    public static List<String> build(List<TestMethod> tests) {
        return tests.stream()
                .map(t -> t.getClassName() + "." + t.getMethodName())
                .distinct()
                .collect(Collectors.toList());
    }
}
