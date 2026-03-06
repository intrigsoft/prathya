package dev.pratya.core.runner;

import dev.pratya.core.model.TestMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SurefireFilterBuilderTest {

    @Test
    void singleTest() {
        List<TestMethod> tests = List.of(
                new TestMethod("com.example.FooTest", "testBar", List.of("REQ-001"))
        );

        assertEquals("com.example.FooTest#testBar", SurefireFilterBuilder.build(tests));
    }

    @Test
    void multipleTests() {
        List<TestMethod> tests = List.of(
                new TestMethod("com.example.FooTest", "test1", List.of("REQ-001")),
                new TestMethod("com.example.BarTest", "test2", List.of("REQ-002"))
        );

        assertEquals("com.example.FooTest#test1+com.example.BarTest#test2",
                SurefireFilterBuilder.build(tests));
    }

    @Test
    void deduplication() {
        List<TestMethod> tests = List.of(
                new TestMethod("com.example.FooTest", "test1", List.of("REQ-001")),
                new TestMethod("com.example.FooTest", "test1", List.of("REQ-002"))
        );

        assertEquals("com.example.FooTest#test1", SurefireFilterBuilder.build(tests));
    }
}
