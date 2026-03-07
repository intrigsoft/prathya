package dev.prathya.core.report;

import dev.prathya.core.model.CodeCoverageSummary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;

public class JacocoReportParser {

    public CodeCoverageSummary parse(Path jacocoXmlFile) throws IOException {
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

            return new CodeCoverageSummary(
                    lineCovered, lineMissed,
                    branchCovered, branchMissed,
                    methodCovered, methodMissed,
                    classCovered, classMissed);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse JaCoCo report: " + jacocoXmlFile, e);
        }
    }
}
