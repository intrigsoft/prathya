package com.intrigsoft.prathya.core.report;

import com.intrigsoft.prathya.core.model.CodeCoverageSummary;
import com.intrigsoft.prathya.core.model.NonContractualEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class JacocoReportParser {

    public CodeCoverageSummary parse(Path jacocoXmlFile) throws IOException {
        return parse(jacocoXmlFile, List.of());
    }

    public CodeCoverageSummary parse(Path jacocoXmlFile, List<NonContractualEntry> exclusions) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document doc = factory.newDocumentBuilder().parse(jacocoXmlFile.toFile());
            Element report = doc.getDocumentElement();

            int lineCovered = 0, lineMissed = 0;
            int branchCovered = 0, branchMissed = 0;
            int methodCovered = 0, methodMissed = 0;
            int classCovered = 0, classMissed = 0;

            // Only read direct child <counter> elements of <report>
            NodeList children = report.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (!(children.item(i) instanceof Element el)) continue;
                if (!"counter".equals(el.getTagName())) continue;

                String type = el.getAttribute("type");
                int covered = Integer.parseInt(el.getAttribute("covered"));
                int missed = Integer.parseInt(el.getAttribute("missed"));

                switch (type) {
                    case "LINE" -> { lineCovered = covered; lineMissed = missed; }
                    case "BRANCH" -> { branchCovered = covered; branchMissed = missed; }
                    case "METHOD" -> { methodCovered = covered; methodMissed = missed; }
                    case "CLASS" -> { classCovered = covered; classMissed = missed; }
                }
            }

            // Subtract counters for @NonContractual exclusions
            if (!exclusions.isEmpty()) {
                Set<String> excludedClasses = new HashSet<>();
                Map<String, Set<String>> excludedMethods = new HashMap<>();
                for (NonContractualEntry entry : exclusions) {
                    String internalName = entry.getClassName().replace('.', '/');
                    if (entry.getMethodName() == null) {
                        excludedClasses.add(internalName);
                    } else {
                        excludedMethods.computeIfAbsent(internalName, k -> new HashSet<>())
                                .add(entry.getMethodName());
                    }
                }

                NodeList packages = report.getElementsByTagName("package");
                for (int p = 0; p < packages.getLength(); p++) {
                    Element pkg = (Element) packages.item(p);
                    NodeList classes = pkg.getElementsByTagName("class");
                    for (int c = 0; c < classes.getLength(); c++) {
                        Element cls = (Element) classes.item(c);
                        String className = cls.getAttribute("name");

                        if (excludedClasses.contains(className)) {
                            // Subtract all counters for this class
                            int[] delta = sumCounters(cls);
                            lineCovered -= delta[0]; lineMissed -= delta[1];
                            branchCovered -= delta[2]; branchMissed -= delta[3];
                            methodCovered -= delta[4]; methodMissed -= delta[5];
                            classCovered -= delta[6]; classMissed -= delta[7];
                        } else if (excludedMethods.containsKey(className)) {
                            Set<String> methodNames = excludedMethods.get(className);
                            NodeList methods = cls.getElementsByTagName("method");
                            for (int m = 0; m < methods.getLength(); m++) {
                                Element method = (Element) methods.item(m);
                                if (methodNames.contains(method.getAttribute("name"))) {
                                    int[] delta = sumCounters(method);
                                    lineCovered -= delta[0]; lineMissed -= delta[1];
                                    branchCovered -= delta[2]; branchMissed -= delta[3];
                                    methodCovered -= delta[4]; methodMissed -= delta[5];
                                    // Methods don't have CLASS counters
                                }
                            }
                        }
                    }
                }
            }

            return new CodeCoverageSummary(
                    lineCovered, lineMissed,
                    branchCovered, branchMissed,
                    methodCovered, methodMissed,
                    classCovered, classMissed);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse JaCoCo report: " + jacocoXmlFile, e);
        }
    }

    /**
     * Sums counter values from direct child {@code <counter>} elements.
     * Returns [lineCov, lineMiss, branchCov, branchMiss, methodCov, methodMiss, classCov, classMiss].
     */
    private int[] sumCounters(Element parent) {
        int[] result = new int[8];
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element el)) continue;
            if (!"counter".equals(el.getTagName())) continue;

            int covered = Integer.parseInt(el.getAttribute("covered"));
            int missed = Integer.parseInt(el.getAttribute("missed"));

            switch (el.getAttribute("type")) {
                case "LINE" -> { result[0] += covered; result[1] += missed; }
                case "BRANCH" -> { result[2] += covered; result[3] += missed; }
                case "METHOD" -> { result[4] += covered; result[5] += missed; }
                case "CLASS" -> { result[6] += covered; result[7] += missed; }
            }
        }
        return result;
    }
}
