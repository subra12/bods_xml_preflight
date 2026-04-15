package com.bods.preflight.analyzer;

import com.bods.preflight.model.ElementDefinition;
import com.bods.preflight.model.Issue;
import com.bods.preflight.model.Severity;
import com.bods.preflight.model.ValidationResult;
import com.bods.preflight.model.XmlAnalysisResult;
import com.bods.preflight.model.XmlElementObservation;
import com.bods.preflight.model.XsdAnalysisResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CrossValidator {
    public ValidationResult validate(XsdAnalysisResult xsd, XmlAnalysisResult xml) {
        ValidationResult result = new ValidationResult();
        evaluateRootMatch(xsd, xml, result);
        evaluateNamespace(xsd, xml, result);
        evaluatePresenceAndNulls(xsd, xml, result);
        evaluateDataTypes(xsd, xml, result);
        return result;
    }

    private void evaluateRootMatch(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        if (xml.getRootElementName().equals(xsd.getRootElementName())) {
            result.setRootMatch(true);
            result.setRootMatchExplanation("PASS (root match - is_top_level_element = 1)");
            return;
        }
        if (xsd.getSingleChildElementName() != null && xml.getRootElementName().equals(xsd.getSingleChildElementName())) {
            result.setRootMatch(true);
            result.setChildRootMatch(true);
            result.setRootMatchExplanation("PASS (child match - is_top_level_element = 0)");
            return;
        }
        result.setRootMatch(false);
        result.setRootMatchExplanation("FAIL (XML root '" + xml.getRootElementName() + "' does not match XSD root '"
                + xsd.getRootElementName() + "' or child '" + xsd.getSingleChildElementName() + "')");
        result.addIssue(new Issue(Severity.ERROR, "CrossValidation", result.getRootMatchExplanation(), null,
                "Review the XML root and the is_top_level_element setting."));
    }

    private void evaluateNamespace(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        String xsdNamespace = emptyToNull(xsd.getRootNamespace());
        String xmlNamespace = emptyToNull(xml.getRootNamespace());
        boolean match = xsdNamespace == null || xmlNamespace == null || xsdNamespace.equals(xmlNamespace);
        result.setNamespaceMatch(match);
        if (!match) {
            result.addIssue(new Issue(Severity.WARNING, "CrossValidation",
                    "Namespace mismatch between XML and XSD.", null, "Ensure XML uses the expected schema namespace."));
        }
    }

    private void evaluatePresenceAndNulls(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        Map<String, XmlElementObservation> observationsByPath = new HashMap<>();
        Set<String> observedNames = new HashSet<>();
        for (XmlElementObservation observation : xml.getElementObservations()) {
            observationsByPath.put(observation.getXPath(), observation);
            observedNames.add(observation.getName());
        }

        for (ElementDefinition definition : xsd.getElements()) {
            if (definition.getXPath().equals("/" + xsd.getRootElementName())) {
                continue;
            }
            String xmlPath = definition.getXPath();
            if (!xmlPath.startsWith("/" + xsd.getRootElementName()) && xsd.getSingleChildElementName() != null) {
                xmlPath = "/" + xsd.getSingleChildElementName() + xmlPath.substring(("/" + xsd.getRootElementName()).length());
            } else if (xmlPath.startsWith("/" + xsd.getRootElementName()) && xsd.getSingleChildElementName() != null
                    && !xml.getRootElementName().equals(xsd.getRootElementName())) {
                xmlPath = "/" + xsd.getSingleChildElementName() + xmlPath.substring(("/" + xsd.getRootElementName() + "/" + xsd.getSingleChildElementName()).length());
            }
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
                        "Populate the required value to avoid BODS row-level NULL output."));
            } else if (!definition.isRequired() && (observation.isEmpty() || observation.hasNilAttribute())) {
                result.addIssue(new Issue(Severity.INFO, "CrossValidation",
                        "Optional XSD element is null or empty in the XML.", xmlPath,
                        "Optional field may be left empty if business rules allow it."));
            }
        }

        Set<String> knownNames = new HashSet<>();
        for (ElementDefinition definition : xsd.getElements()) {
            knownNames.add(definition.getName());
        }
        for (XmlElementObservation observation : xml.getElementObservations()) {
            if (!knownNames.contains(observation.getName())) {
                result.addUnknownXmlElement(observation.getXPath());
                result.addIssue(new Issue(Severity.WARNING, "CrossValidation",
                        "XML element is present in data but not defined in the XSD.", observation.getXPath(),
                        "Remove the element or update the schema."));
            }
        }
    }

    private void evaluateDataTypes(XsdAnalysisResult xsd, XmlAnalysisResult xml, ValidationResult result) {
        Map<String, String> expectedTypes = new HashMap<>();
        for (ElementDefinition definition : xsd.getElements()) {
            String xmlPath = definition.getXPath();
            if (xsd.getSingleChildElementName() != null && !xml.getRootElementName().equals(xsd.getRootElementName())
                    && xmlPath.startsWith("/" + xsd.getRootElementName() + "/" + xsd.getSingleChildElementName())) {
                xmlPath = "/" + xsd.getSingleChildElementName() + xmlPath.substring(("/" + xsd.getRootElementName()
                        + "/" + xsd.getSingleChildElementName()).length());
            }
            expectedTypes.put(xmlPath, definition.getDataType());
        }

        for (XmlElementObservation observation : xml.getElementObservations()) {
            if (observation.hasChildElements() || observation.isEmpty() || observation.hasNilAttribute()) {
                continue;
            }
            String expectedType = expectedTypes.get(observation.getXPath());
            if (expectedType == null || expectedType.isBlank() || "xs:string".equals(expectedType) || "untyped".equals(expectedType)) {
                continue;
            }
            if (!isValidType(observation.getTextValue(), expectedType)) {
                result.addTypeMismatchElement(observation.getXPath());
                result.addIssue(new Issue(Severity.ERROR, "CrossValidation",
                        "XML value does not match expected XSD type '" + expectedType + "': " + observation.getTextValue(),
                        observation.getXPath(), "Correct the value or update the schema definition."));
            }
        }
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
