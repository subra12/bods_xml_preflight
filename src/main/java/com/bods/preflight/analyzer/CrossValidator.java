package com.bods.preflight.analyzer;

import com.bods.preflight.model.ElementDefinition;
import com.bods.preflight.model.Issue;
import com.bods.preflight.model.Severity;
import com.bods.preflight.model.ValidationResult;
import com.bods.preflight.model.XmlAnalysisResult;
import com.bods.preflight.model.XmlElementObservation;
import com.bods.preflight.model.XsdAnalysisResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CrossValidator {
    public ValidationResult validate(XsdAnalysisResult xsd, XmlAnalysisResult xml,
            Path xmlPath, Path xsdPath) {
        ValidationResult result = new ValidationResult();
        evaluateRootMatch(xsd, xml, result);
        evaluateNamespace(xsd, xml, result);
        evaluatePresenceAndNulls(xsd, xml, result);
        evaluateDataTypes(xsd, xml, result);
        evaluateW3cSchema(xmlPath, xsdPath, result);
        return result;
    }

    private void evaluateRootMatch(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        if (xml.getRootElementName().equals(xsd.getRootElementName())) {
            result.setRootMatch(true);
            result.setRootMatchExplanation(
                    "PASS — XML root <" + xml.getRootElementName() + "> matches XSD root element directly. "
                    + "Set is_top_level_element = 1 in BODS.");
            return;
        }
        if (xsd.getSingleChildElementName() != null
                && xml.getRootElementName().equals(xsd.getSingleChildElementName())) {
            result.setRootMatch(true);
            result.setChildRootMatch(true);
            result.setRootMatchExplanation(
                    "PASS — XML root <" + xml.getRootElementName() + "> matches the repeating child element "
                    + "inside XSD wrapper <" + xsd.getRootElementName() + ">. "
                    + "Set is_top_level_element = 0 in BODS.");
            return;
        }
        result.setRootMatch(false);
        String explanation = "FAIL — XML root <" + xml.getRootElementName() + "> does not match "
                + "XSD root <" + xsd.getRootElementName() + ">"
                + (xsd.getSingleChildElementName() != null
                        ? " or its repeating child <" + xsd.getSingleChildElementName() + ">" : "")
                + ". Verify the correct XML file and XSD schema are paired.";
        result.setRootMatchExplanation(explanation);
        result.addIssue(new Issue(Severity.ERROR, "CrossValidation", explanation, null,
                "Check is_top_level_element and ensure the XML root matches the XSD."));
    }

    private void evaluateNamespace(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        String xsdNs = emptyToNull(xsd.getRootNamespace());
        String xmlNs = emptyToNull(xml.getRootNamespace());
        boolean match = xsdNs == null || xmlNs == null || xsdNs.equals(xmlNs);
        result.setNamespaceMatch(match);
        if (!match) {
            String diffHint = buildNamespaceDiffHint(xmlNs, xsdNs);
            result.addIssue(new Issue(Severity.WARNING, "CrossValidation",
                    "Namespace mismatch between XML and XSD root elements."
                    + " XML namespace : \"" + xmlNs + "\""
                    + " | XSD namespace : \"" + xsdNs + "\""
                    + diffHint,
                    null,
                    "Ensure the XML root element uses the namespace declared in the XSD targetNamespace."));
        }
    }

    /**
     * Finds the first character position where the two URIs differ and returns
     * a human-readable hint so the caller can spot typos or trailing slashes.
     */
    private String buildNamespaceDiffHint(String a, String b) {
        if (a == null || b == null) {
            return "";
        }
        int minLen = Math.min(a.length(), b.length());
        for (int i = 0; i < minLen; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return " | First difference at character " + (i + 1)
                        + " (XML='" + a.charAt(i) + "' vs XSD='" + b.charAt(i) + "')";
            }
        }
        if (a.length() != b.length()) {
            // One is a prefix of the other — likely trailing slash or version suffix
            String longer = a.length() > b.length() ? "XML" : "XSD";
            String extra = a.length() > b.length()
                    ? a.substring(minLen) : b.substring(minLen);
            return " | URIs share a common prefix; " + longer + " has extra suffix \"" + extra + "\"";
        }
        return "";
    }

    private void evaluatePresenceAndNulls(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        Map<String, XmlElementObservation> observationsByPath = new HashMap<>();
        Set<String> observedNames = new HashSet<>();
        for (XmlElementObservation observation : xml.getElementObservations()) {
            observationsByPath.put(observation.getXPath(), observation);
            observedNames.add(observation.getName());
        }

        Set<String> knownNames = new HashSet<>();
        for (ElementDefinition definition : xsd.getElements()) {
            knownNames.add(definition.getName());
        }

        for (ElementDefinition definition : xsd.getElements()) {
            if (definition.getXPath().equals("/" + xsd.getRootElementName())) {
                continue;
            }
            String xmlPath = resolveXmlPath(definition.getXPath(), xsd, xml);
            XmlElementObservation observation = observationsByPath.get(xmlPath);
            if (observation == null) {
                if (definition.isRequired()) {
                    result.addMissingRequiredElement(xmlPath);
                    result.addIssue(new Issue(Severity.ERROR, "CrossValidation",
                            "Required XSD element is absent from the XML.", xmlPath,
                            "Populate the required element before loading to BODS."));
                } else {
                    result.addMissingOptionalElement(xmlPath);
                    result.addIssue(new Issue(Severity.INFO, "CrossValidation",
                            "Optional XSD element is absent from the XML.", xmlPath,
                            "No action required unless the field should be present."));
                }
                continue;
            }
            if (definition.isRequired() && (observation.isEmpty() || observation.hasNilAttribute())) {
                result.addRequiredNullElement(xmlPath);
                result.addIssue(new Issue(Severity.CRITICAL, "CrossValidation",
                        "Required XSD element is null or empty in the XML.", xmlPath,
                        "Populate the required value to avoid BODS row-level NULL output.",
                        observation.getLine(), observation.getColumn()));
            } else if (!definition.isRequired() && (observation.isEmpty() || observation.hasNilAttribute())) {
                result.addIssue(new Issue(Severity.INFO, "CrossValidation",
                        "Optional XSD element is null or empty in the XML.", xmlPath,
                        "Optional field may be left empty if business rules allow it.",
                        observation.getLine(), observation.getColumn()));
            }
        }

        for (XmlElementObservation observation : xml.getElementObservations()) {
            if (knownNames.contains(observation.getName())) {
                // Known name — check per-element namespace if the XSD declares one
                String xsdNs = emptyToNull(xsd.getRootNamespace());
                String elemNs = emptyToNull(observation.getNamespaceUri());
                if (xsdNs != null && elemNs != null && !xsdNs.equals(elemNs)) {
                    String diffHint = buildNamespaceDiffHint(elemNs, xsdNs);
                    result.addIssue(new Issue(Severity.WARNING, "CrossValidation",
                            "Element <" + observation.getName() + "> is in namespace \""
                            + elemNs + "\" but the XSD declares namespace \""
                            + xsdNs + "\"." + diffHint,
                            observation.getXPath(),
                            "Ensure the element uses the correct namespace URI.",
                            observation.getLine(), observation.getColumn()));
                }
            } else {
                // Name not in XSD at all — check if it could be a namespace-prefix issue
                String strippedName = stripPrefix(observation.getName());
                if (!strippedName.equals(observation.getName()) && knownNames.contains(strippedName)) {
                    // Qualified name was used in XML but XSD uses the local name — namespace mismatch
                    result.addUnknownXmlElement(observation.getXPath());
                    result.addIssue(new Issue(Severity.WARNING, "CrossValidation",
                            "Element <" + observation.getName() + "> appears to be a namespace-qualified form "
                            + "of <" + strippedName + "> which is defined in the XSD. "
                            + "Remove the namespace prefix or add the namespace to the schema.",
                            observation.getXPath(),
                            "Check the namespace prefix used in the XML against the XSD targetNamespace.",
                            observation.getLine(), observation.getColumn()));
                } else {
                    result.addUnknownXmlElement(observation.getXPath());
                    result.addIssue(new Issue(Severity.WARNING, "CrossValidation",
                            "XML element <" + observation.getName() + "> is present in data but not defined in the XSD.",
                            observation.getXPath(),
                            "Remove the element or update the schema.",
                            observation.getLine(), observation.getColumn()));
                }
            }
        }
    }

    private void evaluateDataTypes(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        Map<String, String> expectedTypes = new HashMap<>();
        for (ElementDefinition definition : xsd.getElements()) {
            String xmlPath = resolveXmlPath(definition.getXPath(), xsd, xml);
            expectedTypes.put(xmlPath, definition.getDataType());
        }

        for (XmlElementObservation observation : xml.getElementObservations()) {
            if (observation.hasChildElements() || observation.isEmpty() || observation.hasNilAttribute()) {
                continue;
            }
            String expectedType = expectedTypes.get(observation.getXPath());
            if (expectedType == null || expectedType.isBlank()
                    || "xs:string".equals(expectedType) || "untyped".equals(expectedType)) {
                continue;
            }
            if (!isValidType(observation.getTextValue(), expectedType)) {
                result.addTypeMismatchElement(observation.getXPath());
                result.addIssue(new Issue(Severity.ERROR, "CrossValidation",
                        "XML value does not match expected XSD type '" + expectedType + "': "
                        + observation.getTextValue(),
                        observation.getXPath(),
                        "Correct the value or update the schema definition.",
                        observation.getLine(), observation.getColumn()));
            }
        }
    }

    private void evaluateW3cSchema(Path xmlPath, Path xsdPath, ValidationResult result) {
        if (result.isChildRootMatch()) {
            // The XML root is the repeating child element (is_top_level_element=0).
            // The W3C validator would always reject this because the XSD root is the
            // wrapper element, not the child. Skip strict validation in this mode.
            result.addIssue(new Issue(Severity.INFO, "W3C",
                    "W3C schema validation skipped: XML root is the repeating child element "
                    + "(is_top_level_element=0). The XSD wrapper root is not present in the XML, "
                    + "which is expected for this BODS pattern.",
                    null, "No action required."));
            return;
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsdPath.toFile()));
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    result.addIssue(new Issue(Severity.WARNING, "W3C",
                            e.getMessage(), null,
                            "Review this schema warning.",
                            e.getLineNumber(), e.getColumnNumber()));
                }

                @Override
                public void error(SAXParseException e) {
                    result.addIssue(new Issue(Severity.ERROR, "W3C",
                            e.getMessage(), null,
                            "Fix this schema violation before loading to BODS.",
                            e.getLineNumber(), e.getColumnNumber()));
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    result.addIssue(new Issue(Severity.CRITICAL, "W3C",
                            e.getMessage(), null,
                            "Fix this critical schema violation before loading to BODS.",
                            e.getLineNumber(), e.getColumnNumber()));
                    throw e;
                }
            });
            validator.validate(new StreamSource(xmlPath.toFile()));
        } catch (SAXParseException ignored) {
            // Already captured by fatalError above.
        } catch (SAXException e) {
            result.addIssue(new Issue(Severity.ERROR, "W3C",
                    "Schema validation failed: " + e.getMessage(), null,
                    "Check the XSD schema for structural errors."));
        } catch (IOException e) {
            result.addIssue(new Issue(Severity.ERROR, "W3C",
                    "Could not read files for W3C schema validation: " + e.getMessage(), null,
                    "Ensure the XML and XSD files are readable."));
        }
    }

    private String resolveXmlPath(String xsdPath, XsdAnalysisResult xsd, XmlAnalysisResult xml) {
        if (xsd.getSingleChildElementName() == null) {
            return xsdPath;
        }
        String xsdRootPrefix = "/" + xsd.getRootElementName();
        String xsdChildPrefix = xsdRootPrefix + "/" + xsd.getSingleChildElementName();
        if (!xml.getRootElementName().equals(xsd.getRootElementName())) {
            // XML root is the child element — strip the wrapper prefix
            if (xsdPath.startsWith(xsdChildPrefix)) {
                return "/" + xsd.getSingleChildElementName()
                        + xsdPath.substring(xsdChildPrefix.length());
            }
            if (xsdPath.startsWith(xsdRootPrefix)) {
                return "/" + xsd.getSingleChildElementName()
                        + xsdPath.substring(xsdRootPrefix.length());
            }
        }
        return xsdPath;
    }

    private String stripPrefix(String name) {
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private boolean isValidType(String value, String xsdType) {
        try {
            switch (xsdType) {
                case "xs:integer":
                    Long.parseLong(value);
                    return true;
                case "xs:decimal":
                    new BigDecimal(value);
                    return true;
                case "xs:date":
                    LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                    return true;
                case "xs:dateTime":
                    OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
                    return true;
                case "xs:boolean":
                    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                            || "1".equals(value) || "0".equals(value);
                default:
                    return true;
            }
        } catch (NumberFormatException | DateTimeParseException exception) {
            return false;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
