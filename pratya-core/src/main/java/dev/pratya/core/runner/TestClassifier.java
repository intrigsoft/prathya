package dev.pratya.core.runner;

import dev.pratya.core.model.TestMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestClassifier {

    private static final List<String> DEFAULT_PATTERNS = Arrays.asList("*IT", "IT*", "*ITCase");

    private final List<String> patterns;

    public TestClassifier() {
        this(DEFAULT_PATTERNS);
    }

    public TestClassifier(List<String> patterns) {
        this.patterns = patterns;
    }

    public TestScope classify(String fullyQualifiedClassName) {
        String simpleName = fullyQualifiedClassName.contains(".")
                ? fullyQualifiedClassName.substring(fullyQualifiedClassName.lastIndexOf('.') + 1)
                : fullyQualifiedClassName;

        for (String pattern : patterns) {
            if (matches(simpleName, pattern)) {
                return TestScope.INTEGRATION;
            }
        }
        return TestScope.UNIT;
    }

    public Map<TestScope, List<TestMethod>> partition(List<TestMethod> tests) {
        return tests.stream().collect(Collectors.groupingBy(
                t -> classify(t.getClassName()),
                Collectors.toList()));
    }

    public List<TestMethod> filter(List<TestMethod> tests, TestScope scope) {
        if (scope == TestScope.ALL) {
            return new ArrayList<>(tests);
        }
        return tests.stream()
                .filter(t -> classify(t.getClassName()) == scope)
                .collect(Collectors.toList());
    }

    private boolean matches(String simpleName, String pattern) {
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            return simpleName.contains(pattern.substring(1, pattern.length() - 1));
        } else if (pattern.startsWith("*")) {
            return simpleName.endsWith(pattern.substring(1));
        } else if (pattern.endsWith("*")) {
            return simpleName.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return simpleName.equals(pattern);
        }
    }
}
