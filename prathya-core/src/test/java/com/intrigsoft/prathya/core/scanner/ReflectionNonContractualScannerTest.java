package com.intrigsoft.prathya.core.scanner;

import com.intrigsoft.prathya.core.model.NonContractualEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionNonContractualScannerTest {

    @Test
    void scanDetectsClassLevelAnnotation() {
        ReflectionNonContractualScanner scanner = new ReflectionNonContractualScanner();
        Path testClassesDir = Paths.get("target/test-classes");
        List<NonContractualEntry> entries = scanner.scan(List.of(testClassesDir));

        // Should find the class-level annotation on NonContractualFixture
        boolean foundClassLevel = entries.stream().anyMatch(e ->
                e.getClassName().endsWith("NonContractualFixture")
                        && e.getMethodName() == null
                        && "DTO class".equals(e.getReason()));
        assertTrue(foundClassLevel, "Should detect class-level @NonContractual on NonContractualFixture");
    }

    @Test
    void scanDetectsMethodLevelAnnotations() {
        ReflectionNonContractualScanner scanner = new ReflectionNonContractualScanner();
        Path testClassesDir = Paths.get("target/test-classes");
        List<NonContractualEntry> entries = scanner.scan(List.of(testClassesDir));

        // Should find method-level annotations on PartialNonContractualFixture
        boolean foundGenerated = entries.stream().anyMatch(e ->
                e.getClassName().endsWith("PartialNonContractualFixture")
                        && "getGeneratedField".equals(e.getMethodName())
                        && "Generated getter".equals(e.getReason()));
        assertTrue(foundGenerated, "Should detect @NonContractual on getGeneratedField");

        boolean foundUtility = entries.stream().anyMatch(e ->
                e.getClassName().endsWith("PartialNonContractualFixture")
                        && "utilityMethod".equals(e.getMethodName())
                        && "Utility method".equals(e.getReason()));
        assertTrue(foundUtility, "Should detect @NonContractual on utilityMethod");
    }

    @Test
    void classLevelAnnotationDoesNotProduceMethodEntries() {
        ReflectionNonContractualScanner scanner = new ReflectionNonContractualScanner();
        Path testClassesDir = Paths.get("target/test-classes");
        List<NonContractualEntry> entries = scanner.scan(List.of(testClassesDir));

        // NonContractualFixture has class-level annotation — should NOT produce method entries
        long methodEntries = entries.stream()
                .filter(e -> e.getClassName().endsWith("NonContractualFixture")
                        && !e.getClassName().contains("Partial")
                        && e.getMethodName() != null)
                .count();
        assertEquals(0, methodEntries,
                "Class-level @NonContractual should not produce individual method entries");
    }

    @Test
    void unannotatedMethodsNotDetected() {
        ReflectionNonContractualScanner scanner = new ReflectionNonContractualScanner();
        Path testClassesDir = Paths.get("target/test-classes");
        List<NonContractualEntry> entries = scanner.scan(List.of(testClassesDir));

        // Unannotated methods on PartialNonContractualFixture should not appear
        boolean foundContractual = entries.stream().anyMatch(e ->
                e.getClassName().endsWith("PartialNonContractualFixture")
                        && "getContractualData".equals(e.getMethodName()));
        assertFalse(foundContractual, "Should not detect unannotated method getContractualData");

        boolean foundCompute = entries.stream().anyMatch(e ->
                e.getClassName().endsWith("PartialNonContractualFixture")
                        && "compute".equals(e.getMethodName()));
        assertFalse(foundCompute, "Should not detect unannotated method compute");
    }
}
