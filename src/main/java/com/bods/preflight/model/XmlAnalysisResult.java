package com.bods.preflight.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlAnalysisResult {
    private String rootElementName;
    private String rootNamespace;
    private long fileSizeBytes;
    private double fileSizeKb;
    private int characterCount;
    private String encoding;
    private String xmlDeclaration;
    private int rootDirectChildCount;
    private boolean singleRepeatingChildType;
    private int totalNullElements;
    private int totalEmptyElements;
    private int maxNestingDepth;
    private final Map<String, Integer> distinctElementCounts = new LinkedHashMap<>();
    private final List<XmlElementObservation> elementObservations = new ArrayList<>();
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

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public double getFileSizeKb() {
        return fileSizeKb;
    }

    public void setFileSizeKb(double fileSizeKb) {
        this.fileSizeKb = fileSizeKb;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(int characterCount) {
        this.characterCount = characterCount;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getXmlDeclaration() {
        return xmlDeclaration;
    }

    public void setXmlDeclaration(String xmlDeclaration) {
        this.xmlDeclaration = xmlDeclaration;
    }

    public int getRootDirectChildCount() {
        return rootDirectChildCount;
    }

    public void setRootDirectChildCount(int rootDirectChildCount) {
        this.rootDirectChildCount = rootDirectChildCount;
    }

    public boolean isSingleRepeatingChildType() {
        return singleRepeatingChildType;
    }

    public void setSingleRepeatingChildType(boolean singleRepeatingChildType) {
        this.singleRepeatingChildType = singleRepeatingChildType;
    }

    public int getTotalNullElements() {
        return totalNullElements;
    }

    public void incrementNullElements() {
        this.totalNullElements++;
    }

    public int getTotalEmptyElements() {
        return totalEmptyElements;
    }

    public void incrementEmptyElements() {
        this.totalEmptyElements++;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        this.maxNestingDepth = maxNestingDepth;
    }

    public Map<String, Integer> getDistinctElementCounts() {
        return Collections.unmodifiableMap(distinctElementCounts);
    }

    public void incrementElementCount(String name) {
        this.distinctElementCounts.merge(name, 1, Integer::sum);
    }

    public List<XmlElementObservation> getElementObservations() {
        return Collections.unmodifiableList(elementObservations);
    }

    public void addObservation(XmlElementObservation observation) {
        this.elementObservations.add(observation);
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void addIssue(Issue issue) {
        this.issues.add(issue);
    }
}
