package com.intrigsoft.prathya.core.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestRunResultTest {

    @Test
    void merge_twoResults() {
        TestRunResult r1 = new TestRunResult(List.of(
                new TestMethodResult("A", "test1", TestMethodResult.TestOutcome.PASSED, null),
                new TestMethodResult("A", "test2", TestMethodResult.TestOutcome.FAILED, "oops")
        ));
        TestRunResult r2 = new TestRunResult(List.of(
                new TestMethodResult("B", "test3", TestMethodResult.TestOutcome.PASSED, null)
        ));

        TestRunResult merged = TestRunResult.merge(r1, r2);

        assertEquals(3, merged.getTotalTests());
        assertEquals(2, merged.getPassed());
        assertEquals(1, merged.getFailed());
        assertFalse(merged.isAllPassing());
    }

    @Test
    void merge_empty() {
        TestRunResult r1 = new TestRunResult(Collections.emptyList());
        TestRunResult r2 = new TestRunResult(Collections.emptyList());

        TestRunResult merged = TestRunResult.merge(r1, r2);

        assertEquals(0, merged.getTotalTests());
        assertFalse(merged.isAllPassing());
    }

    @Test
    void merge_single() {
        TestRunResult r1 = new TestRunResult(List.of(
                new TestMethodResult("A", "test1", TestMethodResult.TestOutcome.PASSED, null)
        ));

        TestRunResult merged = TestRunResult.merge(r1);

        assertEquals(1, merged.getTotalTests());
        assertEquals(1, merged.getPassed());
        assertTrue(merged.isAllPassing());
    }
}
