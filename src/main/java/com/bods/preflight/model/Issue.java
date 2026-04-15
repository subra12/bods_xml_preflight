package com.bods.preflight.model;

public class Issue {
    private final Severity severity;
    private final String category;
    private final String message;
    private final String xPath;
    private final String recommendation;

    public Issue(Severity severity, String category, String message, String xPath, String recommendation) {
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.xPath = xPath;
        this.recommendation = recommendation;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getXPath() {
        return xPath;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
