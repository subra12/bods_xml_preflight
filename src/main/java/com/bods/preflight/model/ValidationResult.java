package com.bods.preflight.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private boolean rootMatch;
    private boolean childRootMatch;
    private boolean namespaceMatch = true;
    private String rootMatchExplanation = "Not evaluated";
    private final List<Issue> issues = new ArrayList<>();
    private final List<String> missingRequiredElements = new ArrayList<>();
    private final List<String> missingOptionalElements = new ArrayList<>();
    private final List<String> unknownXmlElements = new ArrayList<>();
    private final List<String> requiredNullElements = new ArrayList<>();
    private final List<String> typeMismatchElements = new ArrayList<>();

    public boolean isRootMatch() {
        return rootMatch;
    }

    public void setRootMatch(boolean rootMatch) {
        this.rootMatch = rootMatch;
    }

    public boolean isChildRootMatch() {
        return childRootMatch;
    }

    public void setChildRootMatch(boolean childRootMatch) {
        this.childRootMatch = childRootMatch;
    }

    public boolean isNamespaceMatch() {
        return namespaceMatch;
    }

    public void setNamespaceMatch(boolean namespaceMatch) {
        this.namespaceMatch = namespaceMatch;
    }

    public String getRootMatchExplanation() {
        return rootMatchExplanation;
    }

    public void setRootMatchExplanation(String rootMatchExplanation) {
        this.rootMatchExplanation = rootMatchExplanation;
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void addIssue(Issue issue) {
        this.issues.add(issue);
    }

    public List<String> getMissingRequiredElements() {
        return Collections.unmodifiableList(missingRequiredElements);
    }

    public void addMissingRequiredElement(String path) {
        this.missingRequiredElements.add(path);
    }

    public List<String> getMissingOptionalElements() {
        return Collections.unmodifiableList(missingOptionalElements);
    }

    public void addMissingOptionalElement(String path) {
        this.missingOptionalElements.add(path);
    }

    public List<String> getUnknownXmlElements() {
        return Collections.unmodifiableList(unknownXmlElements);
    }

    public void addUnknownXmlElement(String path) {
        this.unknownXmlElements.add(path);
    }

    public List<String> getRequiredNullElements() {
        return Collections.unmodifiableList(requiredNullElements);
    }

    public void addRequiredNullElement(String path) {
        this.requiredNullElements.add(path);
    }

    public List<String> getTypeMismatchElements() {
        return Collections.unmodifiableList(typeMismatchElements);
    }

    public void addTypeMismatchElement(String path) {
        this.typeMismatchElements.add(path);
    }
}
