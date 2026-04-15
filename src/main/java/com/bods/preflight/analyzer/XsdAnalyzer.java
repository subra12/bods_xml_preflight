package com.bods.preflight.analyzer;

import com.bods.preflight.model.ElementDefinition;
import com.bods.preflight.model.Issue;
import com.bods.preflight.model.Severity;
import com.bods.preflight.model.XsdAnalysisResult;
import com.bods.preflight.util.XmlUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XsdAnalyzer {
    public XsdAnalysisResult analyze(Path xsdPath) throws Exception {
        XsdAnalysisResult result = new XsdAnalysisResult();
        Document document = XmlUtils.parseXml(XmlUtils.readAllBytes(xsdPath), "UTF-8");
        Element schema = document.getDocumentElement();
        captureNamespaces(schema, result);
        processImportsAndIncludes(schema, xsdPath, result, new HashSet<>());

        Element rootElement = findFirstChildElement(schema, "element");
        if (rootElement == null) {
            throw new IllegalArgumentException("No top-level xs:element found in XSD.");
        }

        result.setRootElementName(rootElement.getAttribute("name"));
        result.setRootNamespace(schema.getAttribute("targetNamespace"));

        detectRootPattern(rootElement, result);
        traverseElement(rootElement, "/" + rootElement.getAttribute("name"), 1, result);
        result.setTotalElements(result.getElements().size());
        return result;
    }

    private void captureNamespaces(Element schema, XsdAnalysisResult result) {
        NamedNodeMap attributes = schema.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            if (node.getNodeName().startsWith("xmlns")) {
                result.addNamespaceDeclaration(node.getNodeName() + "=" + node.getNodeValue());
            }
        }
    }

    private void processImportsAndIncludes(Element schema, Path xsdPath, XsdAnalysisResult result, Set<String> visited) {
        NodeList children = schema.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element child = (Element) node;
            String ln = localName(child);
            if (!"import".equals(ln) && !"include".equals(ln)) {
                continue;
            }
            String schemaLocation = child.getAttribute("schemaLocation");
            if (schemaLocation == null || schemaLocation.isBlank()) {
                continue;
            }
            try {
                Path importedPath = xsdPath.getParent().resolve(schemaLocation).normalize();
                String canonical = importedPath.toString();
                if (!importedPath.toFile().exists() || visited.contains(canonical)) {
                    continue;
                }
                visited.add(canonical);
                Document importedDoc = XmlUtils.parseXml(XmlUtils.readAllBytes(importedPath), "UTF-8");
                Element importedSchema = importedDoc.getDocumentElement();
                collectImportedElements(importedSchema, result);
                processImportsAndIncludes(importedSchema, importedPath, result, visited);
            } catch (Exception e) {
                result.addIssue(new Issue(Severity.WARNING, "XSD",
                        "Could not resolve imported schema '" + schemaLocation + "': " + e.getMessage(),
                        null, "Ensure the schema file is accessible at the specified location."));
            }
        }
    }

    private void collectImportedElements(Element parent, XsdAnalysisResult result) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element child = (Element) node;
            if (localName(child).equals("element")) {
                String name = child.getAttribute("name");
                if (!name.isBlank()) {
                    boolean alreadyKnown = result.getElements().stream().anyMatch(e -> e.getName().equals(name));
                    if (!alreadyKnown) {
                        String type = child.getAttribute("type");
                        boolean nillable = Boolean.parseBoolean(child.getAttribute("nillable"));
                        result.addElement(new ElementDefinition(name, "/" + name,
                                type.isBlank() ? "untyped" : type,
                                parseOccurs(child.getAttribute("minOccurs"), 1),
                                child.getAttribute("maxOccurs").isBlank() ? "1" : child.getAttribute("maxOccurs"),
                                nillable, false, false, 0));
                    }
                }
            }
            collectImportedElements(child, result);
        }
    }

    private void detectRootPattern(Element rootElement, XsdAnalysisResult result) {
        Element sequence = findDescendant(rootElement, "sequence");
        if (sequence == null) {
            result.setTopLevelElementRecommendationCandidate("1");
            return;
        }
        // Collect only unbounded children — the wrapper pattern requires exactly one.
        List<Element> unboundedChildren = new ArrayList<>();
        NodeList children = sequence.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && localName((Element) child).equals("element")) {
                String maxOccurs = ((Element) child).getAttribute("maxOccurs");
                if ("unbounded".equalsIgnoreCase(maxOccurs)) {
                    unboundedChildren.add((Element) child);
                }
            }
        }
        if (unboundedChildren.size() == 1) {
            Element repeatingChild = unboundedChildren.get(0);
            result.setSingleChildElementName(repeatingChild.getAttribute("name"));
            result.setSingleChildMaxOccurs("unbounded");
            result.setRootHasSingleUnboundedChild(true);
            result.setTopLevelElementRecommendationCandidate("0");
        } else if (unboundedChildren.size() > 1) {
            result.setTopLevelElementRecommendationCandidate("AMBIGUOUS");
            result.addIssue(new Issue(Severity.WARNING, "XSD",
                    "Root element contains multiple unbounded child elements; is_top_level_element is ambiguous.",
                    "/" + result.getRootElementName(), "Review root wrapper design."));
        } else {
            // No unbounded children — root itself is the top-level element.
            result.setTopLevelElementRecommendationCandidate("1");
        }
    }

    private void traverseElement(Element element, String path, int depth, XsdAnalysisResult result) {
        String name = element.getAttribute("name");
        String type = element.getAttribute("type");
        int minOccurs = parseOccurs(element.getAttribute("minOccurs"), 1);
        String maxOccurs = element.getAttribute("maxOccurs");
        if (maxOccurs.isBlank()) {
            maxOccurs = "1";
        }
        boolean nillable = Boolean.parseBoolean(element.getAttribute("nillable"));
        boolean required = minOccurs >= 1 && !nillable;
        boolean complexType = findDescendant(element, "complexType") != null;

        result.addElement(new ElementDefinition(name, path, type.isBlank() ? "untyped" : type, minOccurs, maxOccurs, nillable,
                required, complexType, depth));

        if (required) {
            result.setTotalRequiredElements(result.getTotalRequiredElements() + 1);
        }
        if (minOccurs == 0) {
            result.setTotalOptionalElements(result.getTotalOptionalElements() + 1);
        }
        if (depth > result.getMaxNestingDepth()) {
            result.setMaxNestingDepth(depth);
        }

        if ("xs:anyType".equals(type) || (type.isBlank() && !complexType)) {
            result.addIssue(new Issue(Severity.WARNING, "XSD", "Element uses weak or untyped schema definition.",
                    path, "Consider using a concrete XSD type."));
        }

        Element sequence = findDescendant(element, "sequence");
        if (sequence == null) {
            return;
        }
        NodeList children = sequence.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && localName((Element) child).equals("element")) {
                Element childElement = (Element) child;
                traverseElement(childElement, path + "/" + childElement.getAttribute("name"), depth + 1, result);
            }
        }
    }

    private Element findFirstChildElement(Element parent, String localName) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && localName((Element) node).equals(localName)) {
                return (Element) node;
            }
        }
        return null;
    }

    private Element findDescendant(Element parent, String localName) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                if (localName(child).equals(localName)) {
                    return child;
                }
                Element nested = findDescendant(child, localName);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String localName(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
    }

    private int parseOccurs(String rawValue, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
