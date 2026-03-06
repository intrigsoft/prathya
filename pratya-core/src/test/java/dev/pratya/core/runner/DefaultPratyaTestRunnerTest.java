package dev.pratya.core.runner;

import dev.pratya.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPratyaTestRunnerTest {

    private DefaultPratyaTestRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DefaultPratyaTestRunner();
    }

    // --- resolveTests tests ---

    @Test
    void resolveTests_approvedOnly() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED, cc("REQ-001-CC-001")),
                req("REQ-002", RequirementStatus.DRAFT)
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestA", "test2", "REQ-001-CC-001"),
                trace("TestB", "test3", "REQ-002")
        );

        List<TestMethod> result = runner.resolveTests(contract, traces, RequirementStatus.APPROVED, null);

        assertEquals(2, result.size());
        assertEquals("TestA#test1", result.get(0).toSurefireFilter());
        assertEquals("TestA#test2", result.get(1).toSurefireFilter());
    }

    @Test
    void resolveTests_specificRequirementId() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED),
                req("REQ-002", RequirementStatus.APPROVED)
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "test1", "REQ-001"),
                trace("TestB", "test2", "REQ-002")
        );

        List<TestMethod> result = runner.resolveTests(contract, traces, RequirementStatus.APPROVED, "REQ-002");

        assertEquals(1, result.size());
        assertEquals("TestB#test2", result.get(0).toSurefireFilter());
    }

    @Test
    void resolveTests_cornerCaseInclusion() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED, cc("REQ-001-CC-001"))
        );

        List<TraceEntry> traces = List.of(
                trace("TestA", "testCC", "REQ-001-CC-001")
        );

        List<TestMethod> result = runner.resolveTests(contract, traces, RequirementStatus.APPROVED, null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getRequirementIds().contains("REQ-001-CC-001"));
    }

    @Test
    void resolveTests_emptyTraces() {
        ModuleContract contract = buildContract(
                req("REQ-001", RequirementStatus.APPROVED)
        );

        List<TestMethod> result = runner.resolveTests(contract, Collections.emptyList(),
                RequirementStatus.APPROVED, null);

        assertTrue(result.isEmpty());
    }

    // --- parseResults tests ---

    @Test
    void parseResults_fixtureXml() {
        Path reportsDir = Path.of("src/test/resources/surefire-reports");
        List<TestMethod> expected = List.of(
                new TestMethod("com.example.AuthServiceTest", "testLogin", List.of("REQ-001")),
                new TestMethod("com.example.AuthServiceTest", "testLogout", List.of("REQ-001")),
                new TestMethod("com.example.AuthServiceTest", "testInvalidPassword", List.of("REQ-002")),
                new TestMethod("com.example.AuthServiceTest", "testNullUser", List.of("REQ-002"))
        );

        TestRunResult result = runner.parseResults(reportsDir, expected);

        assertEquals(4, result.getTotalTests());
        assertEquals(2, result.getPassed());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getErrors());

        Map<String, TestMethodResult> resultMap = result.toResultMap();
        assertEquals(TestMethodResult.TestOutcome.PASSED,
                resultMap.get("com.example.AuthServiceTest#testLogin").getOutcome());
        assertEquals(TestMethodResult.TestOutcome.FAILED,
                resultMap.get("com.example.AuthServiceTest#testInvalidPassword").getOutcome());
        assertEquals(TestMethodResult.TestOutcome.ERROR,
                resultMap.get("com.example.AuthServiceTest#testNullUser").getOutcome());
    }

    @Test
    void parseResults_emptyDir(@TempDir Path tempDir) {
        List<TestMethod> expected = List.of(
                new TestMethod("com.example.FooTest", "testBar", List.of("REQ-001"))
        );

        TestRunResult result = runner.parseResults(tempDir, expected);

        assertEquals(1, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(1, result.getSkipped()); // missing tests treated as skipped
    }

    @Test
    void parseResults_allPassing(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.example.FooTest" tests="1" failures="0" errors="0">
                    <testcase classname="com.example.FooTest" name="testBar" time="0.01"/>
                </testsuite>
                """;
        Files.writeString(tempDir.resolve("TEST-com.example.FooTest.xml"), xml);

        List<TestMethod> expected = List.of(
                new TestMethod("com.example.FooTest", "testBar", List.of("REQ-001"))
        );

        TestRunResult result = runner.parseResults(tempDir, expected);

        assertEquals(1, result.getTotalTests());
        assertTrue(result.isAllPassing());
    }

    // --- helpers ---

    private ModuleContract buildContract(RequirementDefinition... reqs) {
        ModuleInfo module = new ModuleInfo();
        module.setId("TEST");
        module.setName("Test Module");
        return new ModuleContract(module, List.of(reqs));
    }

    private RequirementDefinition req(String id, RequirementStatus status, CornerCase... cornerCases) {
        RequirementDefinition r = new RequirementDefinition();
        r.setId(id);
        r.setTitle("Requirement " + id);
        r.setStatus(status);
        r.setCornerCases(List.of(cornerCases));
        return r;
    }

    private CornerCase cc(String id) {
        return new CornerCase(id, "Corner case " + id);
    }

    private TraceEntry trace(String className, String method, String... ids) {
        return new TraceEntry(className, method, List.of(ids));
    }
}
