package com.bods.preflight.analyzer;

import com.bods.preflight.model.DiagnosticReport;
import com.bods.preflight.model.Issue;
import com.bods.preflight.model.ParameterRecommendations;
import com.bods.preflight.model.Severity;
import com.bods.preflight.util.Constants;
import java.util.LinkedHashSet;
import java.util.Set;

public class ParameterRecommender {
    public ParameterRecommendations recommend(DiagnosticReport report) {
        ParameterRecommendations recommendations = new ParameterRecommendations();

        recommendations.setNestedTableName(report.getXmlAnalysis().getRootElementName());
        recommendations.setSchemaDtdName(report.getXsdFile().getFileName().toString());
        recommendations.setActualCharacterCount(report.getXmlAnalysis().getCharacterCount());
        recommendations.setRecommendedMaxSize(calculateRecommendedMaxSize(report.getXmlAnalysis().getCharacterCount()));
        recommendations.setMaxSizeExplanation("actual: " + report.getXmlAnalysis().getCharacterCount()
                + ", buffered: " + (int) Math.ceil(report.getXmlAnalysis().getCharacterCount() * Constants.MAX_SIZE_BUFFER_MULTIPLIER)
                + ". Size production max_size for the largest expected document.");

        if (report.getValidationResult().isChildRootMatch()) {
            recommendations.setIsTopLevelElement("0");
            recommendations.setIsTopLevelElementExplanation("XML root matches the single repeating child in the XSD.");
        } else if (report.getValidationResult().isRootMatch()) {
            recommendations.setIsTopLevelElement("1");
            recommendations.setIsTopLevelElementExplanation("XML root matches the XSD root element.");
        } else {
            recommendations.setIsTopLevelElement("AMBIGUOUS");
            recommendations.setIsTopLevelElementExplanation("XML root did not match the XSD root or single repeating child.");
        }

        if (report.getXmlAnalysis().getTotalNullElements() > 0 || report.getXmlAnalysis().getTotalEmptyElements() > 0) {
            recommendations.setReplaceNullString("''");
            Set<String> uniquePaths = new LinkedHashSet<>();
            for (Issue issue : report.getAllIssues()) {
                if (issue.getXPath() != null && (issue.getMessage().contains("null") || issue.getMessage().contains("empty"))) {
                    uniquePaths.add(issue.getXPath());
                }
            }
            for (String path : uniquePaths) {
                recommendations.addReplaceNullStringAffectedPath(path);
            }
        } else {
            recommendations.setReplaceNullString("NULL");
        }

        String encoding = report.getXmlAnalysis().getEncoding();
        String xmlDeclaration = report.getXmlAnalysis().getXmlDeclaration();
        if (xmlDeclaration == null || xmlDeclaration.isBlank()) {
            recommendations.setXmlHeader("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
            recommendations.setXmlHeaderExplanation("XML declaration is missing, so an explicit header is recommended.");
        } else if ("UTF-8".equalsIgnoreCase(encoding) && xmlDeclaration.contains("UTF-8")) {
            recommendations.setXmlHeader("NULL");
            recommendations.setXmlHeaderExplanation("UTF-8 declaration is standard and compatible with the BODS default.");
        } else {
            recommendations.setXmlHeader(xmlDeclaration);
            recommendations.setXmlHeaderExplanation("Non-UTF-8 encoding requires an explicit xml_header value.");
        }

        boolean hasWarningsOrErrors = report.getAllIssues().stream()
                .anyMatch(issue -> issue.getSeverity() == Severity.CRITICAL
                        || issue.getSeverity() == Severity.ERROR
                        || issue.getSeverity() == Severity.WARNING);
        recommendations.setEnableValidation(hasWarningsOrErrors ? "0" : "1");
        recommendations.setEnableValidationExplanation(hasWarningsOrErrors
                ? "Warnings or errors were found; resolve issues before enabling validation."
                : "All validation checks passed cleanly.");

        return recommendations;
    }

    public int calculateRecommendedMaxSize(int characterCount) {
        return (int) (Math.ceil(characterCount * Constants.MAX_SIZE_BUFFER_MULTIPLIER / Constants.MAX_SIZE_ROUNDING_UNIT)
                * Constants.MAX_SIZE_ROUNDING_UNIT);
    }
}
