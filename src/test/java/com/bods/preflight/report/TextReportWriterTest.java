package com.bods.preflight.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bods.preflight.analyzer.CrossValidator;
import com.bods.preflight.analyzer.ParameterRecommender;
import com.bods.preflight.analyzer.XmlAnalyzer;
import com.bods.preflight.analyzer.XsdAnalyzer;
import com.bods.preflight.model.DiagnosticReport;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class TextReportWriterTest {
    @Test
    void shouldRenderRequiredSectionsInOrder() throws Exception {
        DiagnosticReport report = new DiagnosticReport(
                Paths.get("fixtures/pairs/pass/sample.xml"),
                Paths.get("fixtures/pairs/pass/sample.xsd"),
                false);
        report.setXsdAnalysis(new XsdAnalyzer().analyze(report.getXsdFile()));
        report.setXmlAnalysis(new XmlAnalyzer().analyze(report.getXmlFile(), null));
        report.setValidationResult(new CrossValidator().validate(report.getXsdAnalysis(), report.getXmlAnalysis()));
        report.setParameterRecommendations(new ParameterRecommender().recommend(report));

        String text = new TextReportWriter().write(report);

        assertTrue(text.indexOf("SECTION A: XSD ANALYSIS") < text.indexOf("SECTION B: XML ANALYSIS"));
        assertTrue(text.indexOf("SECTION B: XML ANALYSIS") < text.indexOf("SECTION C: CROSS-VALIDATION"));
        assertTrue(text.indexOf("SECTION C: CROSS-VALIDATION") < text.indexOf("SECTION D: BODS PARAMETER RECOMMENDATIONS"));
        assertTrue(text.contains("SECTION E: ISSUES SUMMARY"));
        assertTrue(text.contains("SECTION F: PRE-FLIGHT STATUS"));
    }
}
