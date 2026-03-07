package dev.prathya.core.report;

import dev.prathya.core.model.CodeCoverageSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
