package com.bods.preflight.model;

import com.bods.preflight.util.Constants;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DiagnosticReport {
    private final String toolName;
    private final String version;
    private final LocalDateTime timestamp;
    private final Path xmlFile;
    private final Path xsdFile;
    private final boolean verbose;
    private XsdAnalysisResult xsdAnalysis;
    private XmlAnalysisResult xmlAnalysis;
    private ValidationResult validationResult;
    private ParameterRecommendations parameterRecommendations;
    private final List<Issue> allIssues = new ArrayList<>();

    public DiagnosticReport(Path xmlFile, Path xsdFile, boolean verbose) {
        this.toolName = Constants.TOOL_NAME;
        this.version = Constants.TOOL_VERSION;
        this.timestamp = LocalDateTime.now();
        this.xmlFile = xmlFile;
        this.xsdFile = xsdFile;
        this.verbose = verbose;
    }

    public String getToolName() {
        return toolName;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Path getXmlFile() {
        return xmlFile;
    }

    public Path getXsdFile() {
        return xsdFile;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public XsdAnalysisResult getXsdAnalysis() {
        return xsdAnalysis;
    }

    public void setXsdAnalysis(XsdAnalysisResult xsdAnalysis) {
        this.xsdAnalysis = xsdAnalysis;
        this.allIssues.addAll(xsdAnalysis.getIssues());
    }

    public XmlAnalysisResult getXmlAnalysis() {
        return xmlAnalysis;
    }

    public void setXmlAnalysis(XmlAnalysisResult xmlAnalysis) {
        this.xmlAnalysis = xmlAnalysis;
        this.allIssues.addAll(xmlAnalysis.getIssues());
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
        this.allIssues.addAll(validationResult.getIssues());
    }

    public ParameterRecommendations getParameterRecommendations() {
        return parameterRecommendations;
    }

    public void setParameterRecommendations(ParameterRecommendations parameterRecommendations) {
        this.parameterRecommendations = parameterRecommendations;
    }

    public List<Issue> getAllIssues() {
        return Collections.unmodifiableList(allIssues);
    }

    public List<Issue> getIssuesBySeverity(Severity severity) {
        List<Issue> filtered = new ArrayList<>();
        for (Issue issue : allIssues) {
            if (issue.getSeverity() == severity) {
                filtered.add(issue);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public Map<Severity, Integer> countBySeverity() {
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity severity : Severity.values()) {
            counts.put(severity, 0);
        }
        for (Issue issue : allIssues) {
            counts.computeIfPresent(issue.getSeverity(), (key, value) -> value + 1);
        }
        return counts;
    }

    public String getOverallStatus() {
        Map<Severity, Integer> counts = countBySeverity();
        if (counts.get(Severity.CRITICAL) > 0 || counts.get(Severity.ERROR) > 0) {
            return Constants.STATUS_FAIL;
        }
        if (counts.get(Severity.WARNING) > 0) {
            return Constants.STATUS_PASS_WITH_WARNINGS;
        }
        return Constants.STATUS_PASS;
    }
}
