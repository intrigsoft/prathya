package dev.pratya.gradle;

import dev.pratya.core.model.TestMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradleTestFilterBuilderTest {

    @Test
    void buildSingleTest() {
        TestMethod method = new TestMethod("com.example.FooTest", "testBar", List.of("REQ-001"));
        List<String> filters = GradleTestFilterBuilder.build(List.of(method));

        assertEquals(1, filters.size());
        assertEquals("com.example.FooTest.testBar", filters.get(0));
    }

    @Test
    void buildMultipleTestsWithDeduplication() {
        TestMethod m1 = new TestMethod("com.example.FooTest", "testBar", List.of("REQ-001"));
        TestMethod m2 = new TestMethod("com.example.FooTest", "testBaz", List.of("REQ-002"));
        TestMethod m3 = new TestMethod("com.example.FooTest", "testBar", List.of("REQ-003")); // duplicate

        List<String> filters = GradleTestFilterBuilder.build(List.of(m1, m2, m3));

        assertEquals(2, filters.size());
        assertTrue(filters.contains("com.example.FooTest.testBar"));
        assertTrue(filters.contains("com.example.FooTest.testBaz"));
    }
}
