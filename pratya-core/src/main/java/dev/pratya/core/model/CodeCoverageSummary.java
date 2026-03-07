package dev.pratya.core.model;

public class CodeCoverageSummary {

    private final int lineCovered;
    private final int lineMissed;
    private final int branchCovered;
    private final int branchMissed;
    private final int methodCovered;
    private final int methodMissed;
    private final int classCovered;
    private final int classMissed;

    public CodeCoverageSummary(int lineCovered, int lineMissed,
                               int branchCovered, int branchMissed,
                               int methodCovered, int methodMissed,
                               int classCovered, int classMissed) {
        this.lineCovered = lineCovered;
        this.lineMissed = lineMissed;
        this.branchCovered = branchCovered;
        this.branchMissed = branchMissed;
        this.methodCovered = methodCovered;
        this.methodMissed = methodMissed;
        this.classCovered = classCovered;
        this.classMissed = classMissed;
    }

    public int getLineCovered() { return lineCovered; }
    public int getLineMissed() { return lineMissed; }
    public int getBranchCovered() { return branchCovered; }
    public int getBranchMissed() { return branchMissed; }
    public int getMethodCovered() { return methodCovered; }
    public int getMethodMissed() { return methodMissed; }
    public int getClassCovered() { return classCovered; }
    public int getClassMissed() { return classMissed; }

    public double getLineRate() {
        int total = lineCovered + lineMissed;
        return total == 0 ? 0.0 : (double) lineCovered / total * 100;
    }

    public double getBranchRate() {
        int total = branchCovered + branchMissed;
        return total == 0 ? 0.0 : (double) branchCovered / total * 100;
    }
}
