package com.bods.preflight.model;

public class Issue {
    private final Severity severity;
    private final String category;
    private final String message;
    private final String xPath;
    private final String recommendation;
    private final int line;
    private final int column;

    public Issue(Severity severity, String category, String message, String xPath, String recommendation) {
        this(severity, category, message, xPath, recommendation, -1, -1);
    }

    public Issue(Severity severity, String category, String message, String xPath, String recommendation,
            int line, int column) {
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.xPath = xPath;
        this.recommendation = recommendation;
        this.line = line;
        this.column = column;
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

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public boolean hasLocation() {
        return line > 0;
    }
}
