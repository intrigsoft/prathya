package dev.pactum.core.runner;

import dev.pactum.core.model.TestMethod;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestClassifierTest {

    private final TestClassifier classifier = new TestClassifier();

    @Test
    void classify_unitTest() {
        assertEquals(TestScope.UNIT, classifier.classify("com.example.FooTest"));
    }

    @Test
    void classify_itSuffix() {
        assertEquals(TestScope.INTEGRATION, classifier.classify("com.example.LoginIT"));
    }

    @Test
    void classify_itPrefix() {
        assertEquals(TestScope.INTEGRATION, classifier.classify("com.example.ITLogin"));
    }

    @Test
    void classify_itCaseSuffix() {
        assertEquals(TestScope.INTEGRATION, classifier.classify("com.example.LoginITCase"));
    }

    @Test
    void classify_regularTestNotIT() {
        assertEquals(TestScope.UNIT, classifier.classify("com.example.ItemTest"));
    }

    @Test
    void classify_simpleNameOnly() {
        assertEquals(TestScope.INTEGRATION, classifier.classify("LoginIT"));
    }

    @Test
    void partition_mixedTests() {
        List<TestMethod> tests = List.of(
                new TestMethod("com.example.FooTest", "test1", List.of("REQ-001")),
                new TestMethod("com.example.LoginIT", "testLogin", List.of("REQ-002")),
                new TestMethod("com.example.BarTest", "test2", List.of("REQ-003")),
                new TestMethod("com.example.ITCheckout", "testCheckout", List.of("REQ-004"))
        );

        Map<TestScope, List<TestMethod>> partitioned = classifier.partition(tests);

        assertEquals(2, partitioned.get(TestScope.UNIT).size());
        assertEquals(2, partitioned.get(TestScope.INTEGRATION).size());
    }

    @Test
    void filter_allScope() {
        List<TestMethod> tests = List.of(
                new TestMethod("com.example.FooTest", "test1", List.of("REQ-001")),
                new TestMethod("com.example.LoginIT", "testLogin", List.of("REQ-002"))
        );

        List<TestMethod> filtered = classifier.filter(tests, TestScope.ALL);
        assertEquals(2, filtered.size());
    }

    @Test
    void filter_unitScope() {
        List<TestMethod> tests = List.of(
                new TestMethod("com.example.FooTest", "test1", List.of("REQ-001")),
                new TestMethod("com.example.LoginIT", "testLogin", List.of("REQ-002"))
        );

        List<TestMethod> filtered = classifier.filter(tests, TestScope.UNIT);
        assertEquals(1, filtered.size());
        assertEquals("com.example.FooTest", filtered.get(0).getClassName());
    }

    @Test
    void customPatterns() {
        TestClassifier custom = new TestClassifier(List.of("*Spec"));
        assertEquals(TestScope.INTEGRATION, custom.classify("com.example.LoginSpec"));
        assertEquals(TestScope.UNIT, custom.classify("com.example.LoginIT"));
    }
}
