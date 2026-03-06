package dev.pactum.core.runner;

import dev.pactum.core.model.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultPactumTestRunner implements PactumTestRunner {

    @Override
    public List<TestMethod> resolveTests(ModuleContract contract, List<TraceEntry> traces,
                                         RequirementStatus statusFilter, String requirementId) {
        // Collect all matching requirement/corner-case IDs
        Set<String> matchingIds = new HashSet<>();
        for (RequirementDefinition req : contract.getRequirements()) {
            if (statusFilter != null && req.getStatus() != statusFilter) {
                continue;
            }
            if (requirementId != null && !req.getId().equals(requirementId)) {
                continue;
            }

            matchingIds.add(req.getId());
            for (CornerCase cc : req.getCornerCases()) {
                matchingIds.add(cc.getId());
            }
        }

        // Filter traces to those referencing any matching ID
        List<TestMethod> result = new ArrayList<>();
        for (TraceEntry trace : traces) {
            List<String> overlapping = trace.getRequirementIds().stream()
                    .filter(matchingIds::contains)
                    .collect(Collectors.toList());
            if (!overlapping.isEmpty()) {
                result.add(new TestMethod(trace.getClassName(), trace.getMethodName(), overlapping));
            }
        }

        return result;
    }

    @Override
    public TestRunResult parseResults(Path surefireReportsDir, List<TestMethod> expectedTests) {
        Map<String, TestMethod> expectedMap = new LinkedHashMap<>();
        for (TestMethod tm : expectedTests) {
            expectedMap.put(tm.toSurefireFilter(), tm);
        }

        Map<String, TestMethodResult> parsedResults = new LinkedHashMap<>();

        if (Files.isDirectory(surefireReportsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(surefireReportsDir, "TEST-*.xml")) {
                for (Path xmlFile : stream) {
                    parseXmlFile(xmlFile, parsedResults);
                }
            } catch (IOException e) {
                // Treat I/O errors as empty results
            }
        }

        // Build final results: match parsed results against expected tests
        List<TestMethodResult> results = new ArrayList<>();
        for (Map.Entry<String, TestMethod> entry : expectedMap.entrySet()) {
            TestMethodResult parsed = parsedResults.get(entry.getKey());
            if (parsed != null) {
                results.add(parsed);
            } else {
                // Test was expected but not found in reports — treat as skipped
                TestMethod tm = entry.getValue();
                results.add(new TestMethodResult(
                        tm.getClassName(), tm.getMethodName(),
                        TestMethodResult.TestOutcome.SKIPPED, null));
            }
        }

        return new TestRunResult(results);
    }

    private void parseXmlFile(Path xmlFile, Map<String, TestMethodResult> results) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile.toFile());

            NodeList testcases = doc.getElementsByTagName("testcase");
            for (int i = 0; i < testcases.getLength(); i++) {
                Element tc = (Element) testcases.item(i);
                String className = tc.getAttribute("classname");
                String methodName = stripParameterizedSuffix(tc.getAttribute("name"));

                TestMethodResult.TestOutcome outcome;
                String failureMessage = null;

                NodeList errors = tc.getElementsByTagName("error");
                NodeList failures = tc.getElementsByTagName("failure");
                NodeList skipped = tc.getElementsByTagName("skipped");

                if (errors.getLength() > 0) {
                    outcome = TestMethodResult.TestOutcome.ERROR;
                    failureMessage = ((Element) errors.item(0)).getAttribute("message");
                } else if (failures.getLength() > 0) {
                    outcome = TestMethodResult.TestOutcome.FAILED;
                    failureMessage = ((Element) failures.item(0)).getAttribute("message");
                } else if (skipped.getLength() > 0) {
                    outcome = TestMethodResult.TestOutcome.SKIPPED;
                } else {
                    outcome = TestMethodResult.TestOutcome.PASSED;
                }

                String key = className + "#" + methodName;
                results.put(key, new TestMethodResult(className, methodName, outcome, failureMessage));
            }
        } catch (Exception e) {
            // Skip unparseable XML files
        }
    }

    private String stripParameterizedSuffix(String name) {
        // JUnit parameterized tests add suffixes like "testMethod[0]" or "testMethod(String)"
        int bracket = name.indexOf('[');
        if (bracket > 0) return name.substring(0, bracket);
        int paren = name.indexOf('(');
        if (paren > 0) return name.substring(0, paren);
        return name;
    }
}
