package com.bods.preflight.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bods.preflight.model.ElementDefinition;
import com.bods.preflight.model.Severity;
import com.bods.preflight.model.XsdAnalysisResult;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class XsdAnalyzerTest {

    private final XsdAnalyzer analyzer = new XsdAnalyzer();

    private Path xsd(String name) throws Exception {
        URL url = XsdAnalyzerTest.class.getClassLoader().getResource("fixtures/xsd/" + name);
        assertNotNull(url, "XSD fixture not found on classpath: fixtures/xsd/" + name);
        return Paths.get(url.toURI());
    }

    // -----------------------------------------------------------------------
    // Root element detection
    // -----------------------------------------------------------------------

    @Test
    void rootDirectShouldProduceRecommendationCandidateOne() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("root-direct.xsd"));

        assertEquals("customer", result.getRootElementName());
        assertFalse(result.isRootHasSingleUnboundedChild());
        assertEquals("1", result.getTopLevelElementRecommendationCandidate());
    }

    @Test
    void rootDirectShouldNotSetSingleChildName() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("root-direct.xsd"));

        // No wrapper pattern — singleChildElementName must not be populated.
        assertTrue(result.getSingleChildElementName() == null
                || result.getSingleChildElementName().isBlank());
    }

    @Test
    void rootWrapperShouldProduceRecommendationCandidateZero() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("root-wrapper-unbounded-child.xsd"));

        assertEquals("allCustomers", result.getRootElementName());
        assertTrue(result.isRootHasSingleUnboundedChild());
        assertEquals("customer", result.getSingleChildElementName());
        assertEquals("unbounded", result.getSingleChildMaxOccurs());
        assertEquals("0", result.getTopLevelElementRecommendationCandidate());
    }

    @Test
    void rootWithMultipleUnboundedChildrenShouldProduceAmbiguous() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("root-ambiguous.xsd"));

        assertEquals("AMBIGUOUS", result.getTopLevelElementRecommendationCandidate());
        assertFalse(result.isRootHasSingleUnboundedChild());

        boolean hasAmbiguousWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING
                        && i.getMessage().contains("ambiguous"));
        assertTrue(hasAmbiguousWarning,
                "Multiple unbounded children must produce an ambiguity WARNING");
    }

    @Test
    void rootWrapperShouldNotProduceAmbiguousWarning() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("root-wrapper-unbounded-child.xsd"));

        boolean hasAmbiguousWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING
                        && i.getMessage().contains("ambiguous"));
        assertFalse(hasAmbiguousWarning,
                "A clean wrapper schema must not produce an ambiguity warning.");
    }

    // -----------------------------------------------------------------------
    // Required vs optional field classification
    // -----------------------------------------------------------------------

    @Test
    void requiredFieldShouldBeClassifiedAsRequired() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("required-optional-fields.xsd"));

        Optional<ElementDefinition> field = result.getElements().stream()
                .filter(e -> "requiredField".equals(e.getName())).findFirst();
        assertTrue(field.isPresent(), "requiredField must be present in element inventory");
        assertTrue(field.get().isRequired());
        assertEquals(1, field.get().getMinOccurs());
    }

    @Test
    void optionalFieldShouldBeClassifiedAsOptional() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("required-optional-fields.xsd"));

        Optional<ElementDefinition> field = result.getElements().stream()
                .filter(e -> "optionalField".equals(e.getName())).findFirst();
        assertTrue(field.isPresent(), "optionalField must be present in element inventory");
        assertFalse(field.get().isRequired());
        assertEquals(0, field.get().getMinOccurs());
    }

    @Test
    void nillableFieldShouldNotBeClassifiedAsRequired() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("required-optional-fields.xsd"));

        Optional<ElementDefinition> field = result.getElements().stream()
                .filter(e -> "nillableField".equals(e.getName())).findFirst();
        assertTrue(field.isPresent(), "nillableField must be present in element inventory");
        assertFalse(field.get().isRequired(),
                "nillable=true must override minOccurs=1 for required classification");
        assertTrue(field.get().isNillable());
    }

    @Test
    void requiredAndOptionalCountsShouldBeCorrect() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("required-optional-fields.xsd"));

        // required: record (root, minOccurs=1 default) + requiredField = 2
        // optional: optionalField = 1
        // nillableField: nillable=true → not required, minOccurs=1 → not optional → 0
        assertEquals(2, result.getTotalRequiredElements());
        assertEquals(1, result.getTotalOptionalElements());
    }

    // -----------------------------------------------------------------------
    // Data type extraction
    // -----------------------------------------------------------------------

    @Test
    void typedFieldsShouldHaveCorrectDataTypes() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("typed-fields.xsd"));

        assertDataType(result, "textField",  "xs:string");
        assertDataType(result, "countField", "xs:integer");
        assertDataType(result, "priceField", "xs:decimal");
        assertDataType(result, "startDate",  "xs:date");
        assertDataType(result, "createdAt",  "xs:dateTime");
        assertDataType(result, "activeFlag", "xs:boolean");
    }

    // -----------------------------------------------------------------------
    // Statistics
    // -----------------------------------------------------------------------

    @Test
    void totalElementsShouldCountAllDefinitions() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("required-optional-fields.xsd"));

        // record + requiredField + optionalField + nillableField = 4
        assertEquals(4, result.getTotalElements());
    }

    @Test
    void maxNestingDepthShouldReflectSchemaDepth() throws Exception {
        XsdAnalysisResult result = analyzer.analyze(xsd("root-direct.xsd"));

        // root (depth 1) → leaf fields (depth 2)
        assertEquals(2, result.getMaxNestingDepth());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void assertDataType(XsdAnalysisResult result, String elementName, String expectedType) {
        Optional<ElementDefinition> field = result.getElements().stream()
                .filter(e -> elementName.equals(e.getName())).findFirst();
        assertTrue(field.isPresent(), elementName + " must be present in element inventory");
        assertEquals(expectedType, field.get().getDataType(),
                "Wrong data type for " + elementName);
    }
}
