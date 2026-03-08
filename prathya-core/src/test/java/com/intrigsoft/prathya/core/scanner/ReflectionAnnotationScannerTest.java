package com.intrigsoft.prathya.core.scanner;

import com.intrigsoft.prathya.core.model.TraceEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionAnnotationScannerTest {

    @Test
    void scanFindsAnnotatedMethods() {
        ReflectionAnnotationScanner scanner = new ReflectionAnnotationScanner();

        // The test classes directory contains the compiled fixture class
        Path testClassesDir = Paths.get("target/test-classes");
        List<TraceEntry> entries = scanner.scan(List.of(testClassesDir));

        // Should find annotated methods from AuthServiceTestFixture
        assertTrue(entries.size() >= 4, "Expected at least 4 annotated methods, got " + entries.size());

        // Check that fixture class is found
        boolean foundLogin = entries.stream().anyMatch(e ->
                e.getMethodName().equals("loginWithValidCredentials")
                        && e.getRequirementIds().contains("AUTH-001"));
        assertTrue(foundLogin, "Should find loginWithValidCredentials annotated with AUTH-001");

        // Check multi-value annotation
        boolean foundMulti = entries.stream().anyMatch(e ->
                e.getMethodName().equals("loginWithUnknownEmail")
                        && e.getRequirementIds().contains("AUTH-001-CC-001")
                        && e.getRequirementIds().contains("AUTH-001-CC-002"));
        assertTrue(foundMulti, "Should find loginWithUnknownEmail with both CC IDs");

        // Unannotated methods should not appear
        boolean foundHelper = entries.stream().anyMatch(e ->
                e.getMethodName().equals("helperMethodWithoutAnnotation"));
        assertFalse(foundHelper, "Should not find unannotated method");
    }
}
