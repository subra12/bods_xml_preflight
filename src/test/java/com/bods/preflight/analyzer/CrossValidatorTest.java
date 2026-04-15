package com.bods.preflight.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bods.preflight.model.Severity;
import com.bods.preflight.model.ValidationResult;
import com.bods.preflight.model.XmlAnalysisResult;
import com.bods.preflight.model.XsdAnalysisResult;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class CrossValidatorTest {

    private final XsdAnalyzer xsdAnalyzer = new XsdAnalyzer();
    private final XmlAnalyzer xmlAnalyzer = new XmlAnalyzer();
    private final CrossValidator validator = new CrossValidator();

    private Path xsd(String name) throws Exception {
        URL url = CrossValidatorTest.class.getClassLoader().getResource("fixtures/xsd/" + name);
        assertNotNull(url, "XSD fixture not found: fixtures/xsd/" + name);
        return Paths.get(url.toURI());
    }

    private Path xml(String name) throws Exception {
        URL url = CrossValidatorTest.class.getClassLoader().getResource("fixtures/xml/" + name);
        assertNotNull(url, "XML fixture not found: fixtures/xml/" + name);
        return Paths.get(url.toURI());
    }

    private ValidationResult validate(String xsdName, String xmlName) throws Exception {
        XsdAnalysisResult xsdResult = xsdAnalyzer.analyze(xsd(xsdName));
        XmlAnalysisResult xmlResult = xmlAnalyzer.analyze(xml(xmlName), null);
        return validator.validate(xsdResult, xmlResult);
    }

    // -----------------------------------------------------------------------
    // Root match logic
    // -----------------------------------------------------------------------

    @Test
    void xmlRootMatchingXsdRootShouldPassWithCandidateOne() throws Exception {
        // root-direct.xsd root=customer; xml-root-matches-root.xml root=customer
        ValidationResult result = validate("root-direct.xsd", "xml-root-matches-root.xml");

        assertTrue(result.isRootMatch());
        assertFalse(result.isChildRootMatch());
        assertTrue(result.getRootMatchExplanation().contains("PASS"));
        assertTrue(result.getRootMatchExplanation().contains("is_top_level_element = 1"));
    }

    @Test
    void xmlRootMatchingXsdChildShouldPassWithCandidateZero() throws Exception {
        // root-wrapper-unbounded-child.xsd root=allCustomers, child=customer
        // xml-root-matches-child.xml root=customer
        ValidationResult result = validate("root-wrapper-unbounded-child.xsd", "xml-root-matches-child.xml");

        assertTrue(result.isRootMatch());
        assertTrue(result.isChildRootMatch());
        assertTrue(result.getRootMatchExplanation().contains("PASS"));
        assertTrue(result.getRootMatchExplanation().contains("is_top_level_element = 0"));
    }

    @Test
    void xmlRootMatchingNeitherShouldFail() throws Exception {
        // root-wrapper-unbounded-child.xsd root=allCustomers, child=customer
        // xml-root-mismatch.xml root=wrongRoot
        ValidationResult result = validate("root-wrapper-unbounded-child.xsd", "xml-root-mismatch.xml");

        assertFalse(result.isRootMatch());
        assertTrue(result.getRootMatchExplanation().contains("FAIL"));
        boolean hasError = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.ERROR);
        assertTrue(hasError, "Root mismatch must produce an ERROR issue");
    }

    // -----------------------------------------------------------------------
    // Required field null checks
    // -----------------------------------------------------------------------

    @Test
    void emptyRequiredFieldShouldProduceCritical() throws Exception {
        ValidationResult result = validate("required-optional-fields.xsd", "xml-required-empty.xml");

        assertFalse(result.getRequiredNullElements().isEmpty(),
                "requiredField is empty — must be in requiredNullElements list");
        boolean hasCritical = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.CRITICAL);
        assertTrue(hasCritical, "Empty required field must produce CRITICAL");
    }

    @Test
    void nilRequiredFieldShouldProduceCritical() throws Exception {
        ValidationResult result = validate("required-optional-fields.xsd", "xml-required-nil.xml");

        assertFalse(result.getRequiredNullElements().isEmpty(),
                "xsi:nil=true on requiredField must be in requiredNullElements list");
        boolean hasCritical = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.CRITICAL);
        assertTrue(hasCritical, "Nil required field must produce CRITICAL");
    }

    @Test
    void emptyOptionalFieldShouldNotProduceCritical() throws Exception {
        ValidationResult result = validate("required-optional-fields.xsd", "xml-optional-empty.xml");

        boolean hasCritical = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.CRITICAL);
        assertFalse(hasCritical, "Empty optional field must NOT produce CRITICAL");
    }

    @Test
    void emptyOptionalFieldShouldProduceInfo() throws Exception {
        ValidationResult result = validate("required-optional-fields.xsd", "xml-optional-empty.xml");

        boolean hasInfo = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.INFO
                        && i.getMessage().toLowerCase().contains("optional"));
        assertTrue(hasInfo, "Empty optional field must produce an INFO issue");
    }

    // -----------------------------------------------------------------------
    // Element presence checks
    // -----------------------------------------------------------------------

    @Test
    void xmlElementAbsentFromXsdShouldProduceWarning() throws Exception {
        // xml-unknown-element.xml has an extra unknownField not defined in required-optional-fields.xsd
        ValidationResult result = validate("required-optional-fields.xsd", "xml-unknown-element.xml");

        assertFalse(result.getUnknownXmlElements().isEmpty(),
                "unknownField must appear in unknownXmlElements list");
        boolean hasWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING
                        && i.getMessage().contains("not defined in the XSD"));
        assertTrue(hasWarning, "Unknown XML element must produce a WARNING");
    }

    @Test
    void requiredXsdElementAbsentFromXmlShouldProduceError() throws Exception {
        // xml-missing-required.xml is missing requiredField entirely
        ValidationResult result = validate("required-optional-fields.xsd", "xml-missing-required.xml");

        assertFalse(result.getMissingRequiredElements().isEmpty(),
                "requiredField must appear in missingRequiredElements list");
        boolean hasError = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.ERROR
                        && i.getMessage().contains("absent from the XML"));
        assertTrue(hasError, "Missing required element must produce an ERROR");
    }

    // -----------------------------------------------------------------------
    // Data type validation
    // -----------------------------------------------------------------------

    @Test
    void typeMismatchedFieldsShouldEachProduceError() throws Exception {
        // xml-type-mismatch.xml has 5 bad-typed fields paired with typed-fields.xsd
        ValidationResult result = validate("typed-fields.xsd", "xml-type-mismatch.xml");

        // textField (xs:string) must NOT produce a mismatch — strings are always valid.
        boolean textFieldMismatch = result.getTypeMismatchElements().stream()
                .anyMatch(p -> p.contains("textField"));
        assertFalse(textFieldMismatch, "xs:string must never produce a type mismatch");

        // All other typed fields have invalid values and must each produce an ERROR.
        long errorCount = result.getIssues().stream()
                .filter(i -> i.getSeverity() == Severity.ERROR
                        && i.getMessage().contains("does not match expected XSD type"))
                .count();
        assertEquals(5, errorCount,
                "5 type-mismatch ERRORs expected (countField, priceField, startDate, createdAt, activeFlag)");
    }

    // -----------------------------------------------------------------------
    // Namespace consistency
    // -----------------------------------------------------------------------

    @Test
    void matchingNamespacesShouldNotProduceWarning() throws Exception {
        // Neither fixture uses a targetNamespace — both have null/empty namespace.
        ValidationResult result = validate("root-direct.xsd", "xml-root-matches-root.xml");

        assertTrue(result.isNamespaceMatch());
        boolean hasNamespaceWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING
                        && i.getMessage().contains("Namespace"));
        assertFalse(hasNamespaceWarning);
    }

    // -----------------------------------------------------------------------
    // xs:import / xs:include — no false unknown-element warnings
    // -----------------------------------------------------------------------

    @Test
    void elementsFromImportedSchemaShouldNotProduceUnknownElementWarning() throws Exception {
        // root-with-import.xsd imports imported-address.xsd which defines address, street, city.
        // xml-with-imported-elements.xml uses all three — none should be flagged as unknown.
        ValidationResult result = validate("root-with-import.xsd", "xml-with-imported-elements.xml");

        boolean hasUnknownWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING
                        && i.getMessage().contains("not defined in the XSD"));
        assertFalse(hasUnknownWarning,
                "Elements defined in an imported schema must not produce unknown-element warnings");
        assertTrue(result.getUnknownXmlElements().isEmpty(),
                "unknownXmlElements list must be empty when all elements are covered by imports");
    }
}
