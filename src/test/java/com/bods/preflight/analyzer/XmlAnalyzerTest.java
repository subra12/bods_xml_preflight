package com.bods.preflight.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bods.preflight.model.Severity;
import com.bods.preflight.model.XmlAnalysisResult;
import com.bods.preflight.model.XmlElementObservation;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class XmlAnalyzerTest {

    private final XmlAnalyzer analyzer = new XmlAnalyzer();

    private Path xml(String name) throws Exception {
        URL url = XmlAnalyzerTest.class.getClassLoader().getResource("fixtures/xml/" + name);
        assertNotNull(url, "XML fixture not found on classpath: fixtures/xml/" + name);
        return Paths.get(url.toURI());
    }

    // -----------------------------------------------------------------------
    // Encoding and XML declaration
    // -----------------------------------------------------------------------

    @Test
    void utf8EncodingShouldBeDetected() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        assertEquals("UTF-8", result.getEncoding());
        assertNotNull(result.getXmlDeclaration(),
                "XML declaration must be captured for UTF-8 files");
        assertTrue(result.getXmlDeclaration().contains("UTF-8"));
    }

    @Test
    void iso8859EncodingShouldBeDetectedFromDeclaration() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-iso-8859-1.xml"), null);

        assertEquals("ISO-8859-1", result.getEncoding());
        assertNotNull(result.getXmlDeclaration());
        assertTrue(result.getXmlDeclaration().contains("ISO-8859-1"));
    }

    @Test
    void nonUtf8EncodingShouldProduceInfoIssue() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-iso-8859-1.xml"), null);

        boolean hasEncodingInfo = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.INFO
                        && i.getMessage().contains("ISO-8859-1"));
        assertTrue(hasEncodingInfo,
                "Non-UTF-8 encoding must produce an INFO issue flagging the explicit xml_header requirement");
    }

    @Test
    void forcedEncodingShouldOverrideDeclaration() throws Exception {
        // Forcing ISO-8859-1 on a UTF-8 file to verify the override mechanism.
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), "ISO-8859-1");

        assertEquals("ISO-8859-1", result.getEncoding());
    }

    // -----------------------------------------------------------------------
    // Character count and max_size warning
    // -----------------------------------------------------------------------

    @Test
    void characterCountBelowThresholdShouldNotProduceMaxSizeWarning() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        assertTrue(result.getCharacterCount() < 4000,
                "xml-utf8.xml must be small enough to stay under the 4000-char threshold");
        boolean hasWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING);
        assertFalse(hasWarning, "No WARNING expected for small XML");
    }

    @Test
    void characterCountAbove4000ShouldProduceMaxSizeWarning() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-over-4000.xml"), null);

        assertTrue(result.getCharacterCount() > 4000,
                "xml-over-4000.xml must exceed 4000 chars");
        boolean hasWarning = result.getIssues().stream()
                .anyMatch(i -> i.getSeverity() == Severity.WARNING);
        assertTrue(hasWarning, "WARNING expected when character count exceeds BODS default max_size");
    }

    @Test
    void fileSizeFieldsShouldBePopulated() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        assertTrue(result.getFileSizeBytes() > 0);
        assertTrue(result.getFileSizeKb() > 0);
    }

    // -----------------------------------------------------------------------
    // Null and empty element detection
    // -----------------------------------------------------------------------

    @Test
    void xsiNilAttributeShouldBeClassifiedAsNull() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-required-nil.xml"), null);

        assertTrue(result.getTotalNullElements() > 0,
                "At least one element must be classified as null (xsi:nil=true)");

        Optional<XmlElementObservation> nilObs = result.getElementObservations().stream()
                .filter(XmlElementObservation::hasNilAttribute).findFirst();
        assertTrue(nilObs.isPresent(), "Observation with nilAttribute=true must exist");
        assertEquals("requiredField", nilObs.get().getName());
    }

    @Test
    void emptySelfClosingElementShouldBeClassifiedAsEmpty() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-required-empty.xml"), null);

        assertTrue(result.getTotalEmptyElements() > 0,
                "At least one element must be classified as empty");

        Optional<XmlElementObservation> emptyObs = result.getElementObservations().stream()
                .filter(o -> o.isEmpty() && !o.hasNilAttribute()).findFirst();
        assertTrue(emptyObs.isPresent(), "Observation with empty=true must exist");
        assertEquals("requiredField", emptyObs.get().getName());
    }

    @Test
    void emptyOptionalElementShouldBeClassifiedAsEmpty() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-optional-empty.xml"), null);

        assertTrue(result.getTotalEmptyElements() > 0);
        Optional<XmlElementObservation> emptyObs = result.getElementObservations().stream()
                .filter(o -> "optionalField".equals(o.getName()) && o.isEmpty()).findFirst();
        assertTrue(emptyObs.isPresent(), "optionalField must be classified as empty");
    }

    @Test
    void nilElementShouldIncrementNullCountNotEmptyCount() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-required-nil.xml"), null);

        assertTrue(result.getTotalNullElements() > 0);
        // A nil element must not also be counted as empty.
        Optional<XmlElementObservation> nilObs = result.getElementObservations().stream()
                .filter(o -> "requiredField".equals(o.getName())).findFirst();
        assertTrue(nilObs.isPresent());
        assertTrue(nilObs.get().hasNilAttribute());
        assertFalse(nilObs.get().isEmpty(),
                "xsi:nil=true must not also be counted as empty");
    }

    // -----------------------------------------------------------------------
    // Root element analysis
    // -----------------------------------------------------------------------

    @Test
    void rootElementNameShouldBeDetected() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        assertEquals("customer", result.getRootElementName());
    }

    @Test
    void rootDirectChildCountShouldBeCorrect() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        // xml-utf8.xml has customerId and email as direct children of customer.
        assertEquals(2, result.getRootDirectChildCount());
    }

    // -----------------------------------------------------------------------
    // Element inventory
    // -----------------------------------------------------------------------

    @Test
    void distinctElementCountsShouldTrackAllElements() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        assertTrue(result.getDistinctElementCounts().containsKey("customer"));
        assertTrue(result.getDistinctElementCounts().containsKey("customerId"));
        assertTrue(result.getDistinctElementCounts().containsKey("email"));
    }

    @Test
    void maxNestingDepthShouldBeTracked() throws Exception {
        XmlAnalysisResult result = analyzer.analyze(xml("xml-utf8.xml"), null);

        // customer (depth 1) → leaf fields (depth 2)
        assertEquals(2, result.getMaxNestingDepth());
    }
}
