package com.intrigsoft.prathya.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeCoverageSummaryTest {

    @Test
    void lineRateComputedCorrectly() {
        CodeCoverageSummary summary = new CodeCoverageSummary(
                120, 45, 0, 0, 0, 0, 0, 0);
        assertEquals(72.7, summary.getLineRate(), 0.1);
    }

    @Test
    void branchRateComputedCorrectly() {
        CodeCoverageSummary summary = new CodeCoverageSummary(
                0, 0, 30, 10, 0, 0, 0, 0);
        assertEquals(75.0, summary.getBranchRate(), 0.1);
    }

    @Test
    void rateIsZeroWhenBothZero() {
        CodeCoverageSummary summary = new CodeCoverageSummary(
                0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(0.0, summary.getLineRate());
        assertEquals(0.0, summary.getBranchRate());
    }

    @Test
    void fullCoverageGives100Percent() {
        CodeCoverageSummary summary = new CodeCoverageSummary(
                100, 0, 50, 0, 0, 0, 0, 0);
        assertEquals(100.0, summary.getLineRate());
        assertEquals(100.0, summary.getBranchRate());
    }

    @Test
    void allFieldsAccessible() {
        CodeCoverageSummary summary = new CodeCoverageSummary(
                120, 45, 30, 10, 25, 5, 8, 2);
        assertEquals(120, summary.getLineCovered());
        assertEquals(45, summary.getLineMissed());
        assertEquals(30, summary.getBranchCovered());
        assertEquals(10, summary.getBranchMissed());
        assertEquals(25, summary.getMethodCovered());
        assertEquals(5, summary.getMethodMissed());
        assertEquals(8, summary.getClassCovered());
        assertEquals(2, summary.getClassMissed());
    }
}
