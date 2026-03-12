package com.intrigsoft.prathya.core.report;

import com.intrigsoft.prathya.core.model.CodeCoverageSummary;
import com.intrigsoft.prathya.core.model.NonContractualEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JacocoReportParserTest {

    private final JacocoReportParser parser = new JacocoReportParser();

    @Test
    void parsesAllCounterTypes() throws IOException {
        Path xmlFile = Path.of("src/test/resources/jacoco-sample.xml");
        CodeCoverageSummary summary = parser.parse(xmlFile);

        assertEquals(120, summary.getLineCovered());
        assertEquals(45, summary.getLineMissed());
        assertEquals(30, summary.getBranchCovered());
        assertEquals(10, summary.getBranchMissed());
        assertEquals(25, summary.getMethodCovered());
        assertEquals(5, summary.getMethodMissed());
        assertEquals(8, summary.getClassCovered());
        assertEquals(2, summary.getClassMissed());
    }

    @Test
    void computesRatesCorrectly() throws IOException {
        Path xmlFile = Path.of("src/test/resources/jacoco-sample.xml");
        CodeCoverageSummary summary = parser.parse(xmlFile);

        // 120 / (120+45) = 72.7%
        assertEquals(72.7, summary.getLineRate(), 0.1);
        // 30 / (30+10) = 75.0%
        assertEquals(75.0, summary.getBranchRate(), 0.1);
    }

    @Test
    void handlesZeroCounters(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <report name="empty">
                    <counter type="LINE" missed="0" covered="0"/>
                    <counter type="BRANCH" missed="0" covered="0"/>
                    <counter type="METHOD" missed="0" covered="0"/>
                    <counter type="CLASS" missed="0" covered="0"/>
                </report>
                """;
        Path xmlFile = tempDir.resolve("jacoco.xml");
        Files.writeString(xmlFile, xml);

        CodeCoverageSummary summary = parser.parse(xmlFile);

        assertEquals(0.0, summary.getLineRate());
        assertEquals(0.0, summary.getBranchRate());
    }

    @Test
    void handlesMissingCounterTypes(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <report name="partial">
                    <counter type="LINE" missed="10" covered="90"/>
                </report>
                """;
        Path xmlFile = tempDir.resolve("jacoco.xml");
        Files.writeString(xmlFile, xml);

        CodeCoverageSummary summary = parser.parse(xmlFile);

        assertEquals(90, summary.getLineCovered());
        assertEquals(10, summary.getLineMissed());
        assertEquals(0, summary.getBranchCovered());
        assertEquals(0, summary.getBranchMissed());
        assertEquals(0, summary.getMethodCovered());
        assertEquals(0, summary.getMethodMissed());
        assertEquals(0, summary.getClassCovered());
        assertEquals(0, summary.getClassMissed());
    }

    @Test
    void parseWithEmptyExclusionsMatchesOriginal() throws IOException {
        Path xmlFile = Path.of("src/test/resources/jacoco-sample.xml");
        CodeCoverageSummary withEmpty = parser.parse(xmlFile, List.of());
        CodeCoverageSummary without = parser.parse(xmlFile);

        assertEquals(without.getLineCovered(), withEmpty.getLineCovered());
        assertEquals(without.getLineMissed(), withEmpty.getLineMissed());
        assertEquals(without.getBranchCovered(), withEmpty.getBranchCovered());
        assertEquals(without.getBranchMissed(), withEmpty.getBranchMissed());
    }

    @Test
    void parseExcludesWholeClass() throws IOException {
        // jacoco-sample-with-classes.xml totals: LINE 4m/16c, BRANCH 1m/5c, METHOD 1m/4c, CLASS 0m/3c
        // AppDto class counters: LINE 1m/1c, METHOD 1m/1c, CLASS 0m/1c (no BRANCH)
        Path xmlFile = Path.of("src/test/resources/jacoco-sample-with-classes.xml");
        List<NonContractualEntry> exclusions = List.of(
                new NonContractualEntry("com.example.AppDto", null, "DTO class")
        );

        CodeCoverageSummary summary = parser.parse(xmlFile, exclusions);

        // LINE: 16-1=15 covered, 4-1=3 missed
        assertEquals(15, summary.getLineCovered());
        assertEquals(3, summary.getLineMissed());
        // BRANCH: unchanged (AppDto has no branch counters)
        assertEquals(5, summary.getBranchCovered());
        assertEquals(1, summary.getBranchMissed());
        // METHOD: 4-1=3 covered, 1-1=0 missed
        assertEquals(3, summary.getMethodCovered());
        assertEquals(0, summary.getMethodMissed());
        // CLASS: 3-1=2 covered, 0 missed
        assertEquals(2, summary.getClassCovered());
        assertEquals(0, summary.getClassMissed());
    }

    @Test
    void parseExcludesSpecificMethod() throws IOException {
        // Exclude only AppService.validate method
        // validate counters: LINE 1m/4c, BRANCH 1m/1c, METHOD 0m/1c
        Path xmlFile = Path.of("src/test/resources/jacoco-sample-with-classes.xml");
        List<NonContractualEntry> exclusions = List.of(
                new NonContractualEntry("com.example.AppService", "validate", "Internal method")
        );

        CodeCoverageSummary summary = parser.parse(xmlFile, exclusions);

        // LINE: 16-4=12 covered, 4-1=3 missed
        assertEquals(12, summary.getLineCovered());
        assertEquals(3, summary.getLineMissed());
        // BRANCH: 5-1=4 covered, 1-1=0 missed
        assertEquals(4, summary.getBranchCovered());
        assertEquals(0, summary.getBranchMissed());
        // METHOD: 4-1=3 covered, 1 missed (method counters don't affect class)
        assertEquals(3, summary.getMethodCovered());
        assertEquals(1, summary.getMethodMissed());
        // CLASS: unchanged (method exclusion doesn't affect class counter)
        assertEquals(3, summary.getClassCovered());
        assertEquals(0, summary.getClassMissed());
    }

    @Test
    void parseExcludesClassAndMethodCombined() throws IOException {
        // Exclude AppDto (whole class) and AppService.validate (method)
        Path xmlFile = Path.of("src/test/resources/jacoco-sample-with-classes.xml");
        List<NonContractualEntry> exclusions = List.of(
                new NonContractualEntry("com.example.AppDto", null, "DTO"),
                new NonContractualEntry("com.example.AppService", "validate", "Internal")
        );

        CodeCoverageSummary summary = parser.parse(xmlFile, exclusions);

        // LINE: 16-1-4=11 covered, 4-1-1=2 missed
        assertEquals(11, summary.getLineCovered());
        assertEquals(2, summary.getLineMissed());
        // BRANCH: 5-1=4 covered, 1-1=0 missed
        assertEquals(4, summary.getBranchCovered());
        assertEquals(0, summary.getBranchMissed());
        // METHOD: 4-1-1=2 covered, 1-1-0=0 missed
        assertEquals(2, summary.getMethodCovered());
        assertEquals(0, summary.getMethodMissed());
        // CLASS: 3-1=2 covered
        assertEquals(2, summary.getClassCovered());
        assertEquals(0, summary.getClassMissed());
    }

    @Test
    void parseWithNonMatchingExclusionLeavesTotalsUnchanged() throws IOException {
        Path xmlFile = Path.of("src/test/resources/jacoco-sample-with-classes.xml");
        List<NonContractualEntry> exclusions = List.of(
                new NonContractualEntry("com.example.DoesNotExist", null, "ghost")
        );

        CodeCoverageSummary summary = parser.parse(xmlFile, exclusions);

        assertEquals(16, summary.getLineCovered());
        assertEquals(4, summary.getLineMissed());
        assertEquals(5, summary.getBranchCovered());
        assertEquals(1, summary.getBranchMissed());
    }
}
