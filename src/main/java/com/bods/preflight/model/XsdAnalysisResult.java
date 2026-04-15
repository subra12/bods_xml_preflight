package com.bods.preflight.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XsdAnalysisResult {
    private String rootElementName;
    private String rootNamespace;
    private String singleChildElementName;
    private String singleChildMaxOccurs;
    private boolean rootHasSingleUnboundedChild;
    private int totalElements;
    private int totalRequiredElements;
    private int totalOptionalElements;
    private int maxNestingDepth;
    private String topLevelElementRecommendationCandidate = "AMBIGUOUS";
    private final List<String> namespaceDeclarations = new ArrayList<>();
    private final List<ElementDefinition> elements = new ArrayList<>();
    private final List<Issue> issues = new ArrayList<>();

    public String getRootElementName() {
        return rootElementName;
    }

    public void setRootElementName(String rootElementName) {
        this.rootElementName = rootElementName;
    }

    public String getRootNamespace() {
        return rootNamespace;
    }

    public void setRootNamespace(String rootNamespace) {
        this.rootNamespace = rootNamespace;
    }

    public String getSingleChildElementName() {
        return singleChildElementName;
    }

    public void setSingleChildElementName(String singleChildElementName) {
        this.singleChildElementName = singleChildElementName;
    }

    public String getSingleChildMaxOccurs() {
        return singleChildMaxOccurs;
    }

    public void setSingleChildMaxOccurs(String singleChildMaxOccurs) {
        this.singleChildMaxOccurs = singleChildMaxOccurs;
    }

    public boolean isRootHasSingleUnboundedChild() {
        return rootHasSingleUnboundedChild;
    }

    public void setRootHasSingleUnboundedChild(boolean rootHasSingleUnboundedChild) {
        this.rootHasSingleUnboundedChild = rootHasSingleUnboundedChild;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalRequiredElements() {
        return totalRequiredElements;
    }

    public void setTotalRequiredElements(int totalRequiredElements) {
        this.totalRequiredElements = totalRequiredElements;
    }

    public int getTotalOptionalElements() {
        return totalOptionalElements;
    }

    public void setTotalOptionalElements(int totalOptionalElements) {
        this.totalOptionalElements = totalOptionalElements;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        this.maxNestingDepth = maxNestingDepth;
    }

    public String getTopLevelElementRecommendationCandidate() {
        return topLevelElementRecommendationCandidate;
    }

    public void setTopLevelElementRecommendationCandidate(String candidate) {
        this.topLevelElementRecommendationCandidate = candidate;
    }

    public List<String> getNamespaceDeclarations() {
        return Collections.unmodifiableList(namespaceDeclarations);
    }

    public void addNamespaceDeclaration(String namespaceDeclaration) {
        this.namespaceDeclarations.add(namespaceDeclaration);
    }

    public List<ElementDefinition> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void addElement(ElementDefinition elementDefinition) {
        this.elements.add(elementDefinition);
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void addIssue(Issue issue) {
        this.issues.add(issue);
    }
}
