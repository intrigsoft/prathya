package com.intrigsoft.prathya.core.runner;

import com.intrigsoft.prathya.core.model.TestMethod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public final class SurefireFilterBuilder {

    private SurefireFilterBuilder() {}

    public static String build(List<TestMethod> tests) {
        // Group methods by class: ClassName -> [method1, method2]
        Map<String, List<TestMethod>> byClass = tests.stream()
                .collect(Collectors.groupingBy(TestMethod::getClassName, LinkedHashMap::new, Collectors.toList()));

        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, List<TestMethod>> entry : byClass.entrySet()) {
            String methods = entry.getValue().stream()
                    .map(TestMethod::getMethodName)
                    .distinct()
                    .collect(Collectors.joining("+"));
            joiner.add(entry.getKey() + "#" + methods);
        }
        return joiner.toString();
    }
}
