package com.bods.preflight.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParameterRecommendations {
    private String nestedTableName;
    private String schemaDtdName;
    private String isTopLevelElement;
    private String isTopLevelElementExplanation;
    private int actualCharacterCount;
    private int recommendedMaxSize;
    private String maxSizeExplanation;
    private String replaceNullString;
    private final List<String> replaceNullStringAffectedPaths = new ArrayList<>();
    private String xmlHeader;
    private String xmlHeaderExplanation;
    private String enableValidation;
    private String enableValidationExplanation;

    public String getNestedTableName() {
        return nestedTableName;
    }

    public void setNestedTableName(String nestedTableName) {
        this.nestedTableName = nestedTableName;
    }

    public String getSchemaDtdName() {
        return schemaDtdName;
    }

    public void setSchemaDtdName(String schemaDtdName) {
        this.schemaDtdName = schemaDtdName;
    }

    public String getIsTopLevelElement() {
        return isTopLevelElement;
    }

    public void setIsTopLevelElement(String isTopLevelElement) {
        this.isTopLevelElement = isTopLevelElement;
    }

    public String getIsTopLevelElementExplanation() {
        return isTopLevelElementExplanation;
    }

    public void setIsTopLevelElementExplanation(String isTopLevelElementExplanation) {
        this.isTopLevelElementExplanation = isTopLevelElementExplanation;
    }

    public int getActualCharacterCount() {
        return actualCharacterCount;
    }

    public void setActualCharacterCount(int actualCharacterCount) {
        this.actualCharacterCount = actualCharacterCount;
    }

    public int getRecommendedMaxSize() {
        return recommendedMaxSize;
    }

    public void setRecommendedMaxSize(int recommendedMaxSize) {
        this.recommendedMaxSize = recommendedMaxSize;
    }

    public String getMaxSizeExplanation() {
        return maxSizeExplanation;
    }

    public void setMaxSizeExplanation(String maxSizeExplanation) {
        this.maxSizeExplanation = maxSizeExplanation;
    }

    public String getReplaceNullString() {
        return replaceNullString;
    }

    public void setReplaceNullString(String replaceNullString) {
        this.replaceNullString = replaceNullString;
    }

    public List<String> getReplaceNullStringAffectedPaths() {
        return Collections.unmodifiableList(replaceNullStringAffectedPaths);
    }

    public void addReplaceNullStringAffectedPath(String path) {
        this.replaceNullStringAffectedPaths.add(path);
    }

    public String getXmlHeader() {
        return xmlHeader;
    }

    public void setXmlHeader(String xmlHeader) {
        this.xmlHeader = xmlHeader;
    }

    public String getXmlHeaderExplanation() {
        return xmlHeaderExplanation;
    }

    public void setXmlHeaderExplanation(String xmlHeaderExplanation) {
        this.xmlHeaderExplanation = xmlHeaderExplanation;
    }

    public String getEnableValidation() {
        return enableValidation;
    }

    public void setEnableValidation(String enableValidation) {
        this.enableValidation = enableValidation;
    }

    public String getEnableValidationExplanation() {
        return enableValidationExplanation;
    }

    public void setEnableValidationExplanation(String enableValidationExplanation) {
        this.enableValidationExplanation = enableValidationExplanation;
    }
}
