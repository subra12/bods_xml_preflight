package com.bods.preflight.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bods.preflight.analyzer.CrossValidator;
import com.bods.preflight.analyzer.ParameterRecommender;
import com.bods.preflight.analyzer.XmlAnalyzer;
import com.bods.preflight.analyzer.XsdAnalyzer;
import com.bods.preflight.model.DiagnosticReport;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class HtmlReportWriterTest {
    @Test
    void shouldRenderHtmlSections() throws Exception {
        DiagnosticReport report = new DiagnosticReport(
                Paths.get("fixtures/pairs/pass/sample.xml"),
                Paths.get("fixtures/pairs/pass/sample.xsd"),
                false);
        report.setXsdAnalysis(new XsdAnalyzer().analyze(report.getXsdFile()));
        report.setXmlAnalysis(new XmlAnalyzer().analyze(report.getXmlFile(), null));
        report.setValidationResult(new CrossValidator().validate(report.getXsdAnalysis(), report.getXmlAnalysis()));
        report.setParameterRecommendations(new ParameterRecommender().recommend(report));

        String html = new HtmlReportWriter().write(report);

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("SECTION A: XSD ANALYSIS"));
        assertTrue(html.contains("SECTION D: BODS PARAMETER RECOMMENDATIONS"));
        assertTrue(html.contains("SECTION F: PRE-FLIGHT STATUS"));
    }
}
