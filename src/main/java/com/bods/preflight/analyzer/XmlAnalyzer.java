package com.bods.preflight.analyzer;

import com.bods.preflight.model.Issue;
import com.bods.preflight.model.Severity;
import com.bods.preflight.model.XmlAnalysisResult;
import com.bods.preflight.model.XmlElementObservation;
import com.bods.preflight.util.Constants;
import com.bods.preflight.util.XmlUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

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

        Map<String, int[]> locationMap = buildLocationMap(bytes, encoding);

        Document document = XmlUtils.parseXml(bytes, encoding);
        Element root = document.getDocumentElement();
        result.setRootElementName(root.getLocalName() != null ? root.getLocalName() : root.getNodeName());
        result.setRootNamespace(root.getNamespaceURI());
        result.setRootDirectChildCount(countChildElements(root));
        result.setSingleRepeatingChildType(hasSingleChildType(root));
        traverse(root, "/" + result.getRootElementName(), 1, result, locationMap);
        return result;
    }

    private Map<String, int[]> buildLocationMap(byte[] bytes, String encoding) {
        Map<String, int[]> map = new HashMap<>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                private Locator locator;
                private final Deque<String> pathStack = new ArrayDeque<>();

                @Override
                public void setDocumentLocator(Locator locator) {
                    this.locator = locator;
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attrs) {
                    String name = localName != null && !localName.isBlank() ? localName : qName;
                    String parentPath = pathStack.isEmpty() ? "" : pathStack.peek();
                    String currentPath = parentPath + "/" + name;
                    if (!map.containsKey(currentPath) && locator != null) {
                        map.put(currentPath, new int[]{locator.getLineNumber(), locator.getColumnNumber()});
                    }
                    pathStack.push(currentPath);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (!pathStack.isEmpty()) {
                        pathStack.pop();
                    }
                }
            };
            String decoded = XmlUtils.decode(bytes, encoding);
            parser.parse(new ByteArrayInputStream(decoded.getBytes(StandardCharsets.UTF_8)), handler);
        } catch (Exception ignored) {
            // Location map is best-effort; DOM parsing will still proceed normally.
        }
        return map;
    }

    private void traverse(Element element, String path, int depth, XmlAnalysisResult result,
            Map<String, int[]> locationMap) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
        boolean hasChildElements = countChildElements(element) > 0;
        boolean hasTextContent = !text.isBlank() && !hasChildElements;
        boolean nilAttribute = "true".equalsIgnoreCase(element.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "nil"))
                || "true".equalsIgnoreCase(element.getAttribute("xsi:nil"));
        boolean empty = !hasChildElements && !hasTextContent && !nilAttribute;

        int[] loc = locationMap.getOrDefault(path, new int[]{-1, -1});
        String namespaceUri = element.getNamespaceURI();

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

        XmlElementObservation observation = new XmlElementObservation(
                name, path, text, hasTextContent, hasChildElements, empty, nilAttribute, depth,
                namespaceUri, loc[0], loc[1]);
        result.addObservation(observation);

        if (nilAttribute || empty) {
            result.addIssue(new Issue(Severity.INFO, "XML", "XML element is null or empty.", path,
                    "Review whether this element is allowed to be null in the schema.",
                    loc[0], loc[1]));
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                String childName = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                traverse(child, path + "/" + childName, depth + 1, result, locationMap);
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
