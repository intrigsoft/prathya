package com.intrigsoft.prathya.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestRunResult {

    private final List<TestMethodResult> results;

    public TestRunResult(List<TestMethodResult> results) {
        this.results = results;
    }

    public List<TestMethodResult> getResults() { return results; }

    public int getTotalTests() { return results.size(); }

    public long getPassed() {
        return results.stream().filter(r -> r.getOutcome() == TestMethodResult.TestOutcome.PASSED).count();
    }

    public long getFailed() {
        return results.stream().filter(r -> r.getOutcome() == TestMethodResult.TestOutcome.FAILED).count();
    }

    public long getErrors() {
        return results.stream().filter(r -> r.getOutcome() == TestMethodResult.TestOutcome.ERROR).count();
    }

    public long getSkipped() {
        return results.stream().filter(r -> r.getOutcome() == TestMethodResult.TestOutcome.SKIPPED).count();
    }

    public boolean isAllPassing() {
        return !results.isEmpty() && results.stream()
                .allMatch(r -> r.getOutcome() == TestMethodResult.TestOutcome.PASSED);
    }

    public static TestRunResult merge(TestRunResult... results) {
        List<TestMethodResult> all = new ArrayList<>();
        for (TestRunResult r : results) {
            all.addAll(r.getResults());
        }
        return new TestRunResult(all);
    }

    public Map<String, TestMethodResult> toResultMap() {
        return results.stream().collect(Collectors.toMap(
                r -> r.getClassName() + "#" + r.getMethodName(),
                r -> r,
                (a, b) -> a));
    }
}
