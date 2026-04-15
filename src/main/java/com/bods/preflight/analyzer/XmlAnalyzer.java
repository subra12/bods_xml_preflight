package com.bods.preflight.analyzer;

import com.bods.preflight.model.Issue;
import com.bods.preflight.model.Severity;
import com.bods.preflight.model.XmlAnalysisResult;
import com.bods.preflight.model.XmlElementObservation;
import com.bods.preflight.util.Constants;
import com.bods.preflight.util.XmlUtils;
import java.nio.file.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlAnalyzer {
    public XmlAnalysisResult analyze(Path xmlPath, String forcedEncoding) throws Exception {
        XmlAnalysisResult result = new XmlAnalysisResult();
        byte[] bytes = XmlUtils.readAllBytes(xmlPath);
        String declaration = XmlUtils.extractXmlDeclaration(bytes);
        String encoding = XmlUtils.detectEncoding(declaration, forcedEncoding);
        String content = XmlUtils.decode(bytes, encoding);

        result.setFileSizeBytes(bytes.length);
        result.setFileSizeKb(bytes.length / 1024.0d);
        result.setCharacterCount(content.length());
        result.setEncoding(encoding);
        result.setXmlDeclaration(declaration);

        if (result.getCharacterCount() > Constants.BODS_DEFAULT_MAX_SIZE) {
            result.addIssue(new Issue(Severity.WARNING, "XML", "XML character count exceeds the BODS default max_size of 4000.",
                    null, "Set max_size explicitly in BODS."));
        }

        if (!"UTF-8".equalsIgnoreCase(encoding) && !"US-ASCII".equalsIgnoreCase(encoding)) {
            result.addIssue(new Issue(Severity.INFO, "XML",
                    "XML encoding is " + encoding + ". BODS requires an explicit xml_header parameter for non-UTF-8 documents.",
                    null, "Set xml_header in load_to_xml() to the exact XML declaration string."));
        }

        Document document = XmlUtils.parseXml(bytes, encoding);
        Element root = document.getDocumentElement();
        result.setRootElementName(root.getLocalName() != null ? root.getLocalName() : root.getNodeName());
        result.setRootNamespace(root.getNamespaceURI());
        result.setRootDirectChildCount(countChildElements(root));
        result.setSingleRepeatingChildType(hasSingleChildType(root));
        traverse(root, "/" + result.getRootElementName(), 1, result);
        return result;
    }

    private void traverse(Element element, String path, int depth, XmlAnalysisResult result) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
        boolean hasChildElements = countChildElements(element) > 0;
        boolean hasTextContent = !text.isBlank() && !hasChildElements;
        boolean nilAttribute = "true".equalsIgnoreCase(element.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "nil"))
                || "true".equalsIgnoreCase(element.getAttribute("xsi:nil"));
        boolean empty = !hasChildElements && !hasTextContent && !nilAttribute;

        result.incrementElementCount(name);
        if (depth > result.getMaxNestingDepth()) {
            result.setMaxNestingDepth(depth);
        }
        if (nilAttribute) {
            result.incrementNullElements();
        }
        if (empty) {
            result.incrementEmptyElements();
        }

        XmlElementObservation observation =
                new XmlElementObservation(name, path, text, hasTextContent, hasChildElements, empty, nilAttribute, depth);
        result.addObservation(observation);

        if (nilAttribute || empty) {
            Severity severity = Severity.INFO;
            result.addIssue(new Issue(severity, "XML", "XML element is null or empty.", path,
                    "Review whether this element is allowed to be null in the schema."));
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                String childName = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                traverse(child, path + "/" + childName, depth + 1, result);
            }
        }
    }

    private int countChildElements(Element element) {
        int count = 0;
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                count++;
            }
        }
        return count;
    }

    private boolean hasSingleChildType(Element root) {
        String onlyName = null;
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                Element child = (Element) nodes.item(i);
                String name = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                if (onlyName == null) {
                    onlyName = name;
                } else if (!onlyName.equals(name)) {
                    return false;
                }
            }
        }
        return onlyName != null;
    }
}
