package com.bods.preflight.model;

public class XmlElementObservation {
    private final String name;
    private final String xPath;
    private final String textValue;
    private final boolean hasTextContent;
    private final boolean hasChildElements;
    private final boolean empty;
    private final boolean nilAttribute;
    private final int depth;

    public XmlElementObservation(String name, String xPath, String textValue, boolean hasTextContent,
            boolean hasChildElements, boolean empty, boolean nilAttribute, int depth) {
        this.name = name;
        this.xPath = xPath;
        this.textValue = textValue;
        this.hasTextContent = hasTextContent;
        this.hasChildElements = hasChildElements;
        this.empty = empty;
        this.nilAttribute = nilAttribute;
        this.depth = depth;
    }

    public String getName() {
        return name;
    }

    public String getXPath() {
        return xPath;
    }

    public String getTextValue() {
        return textValue;
    }

    public boolean hasTextContent() {
        return hasTextContent;
    }

    public boolean hasChildElements() {
        return hasChildElements;
    }

    public boolean isEmpty() {
        return empty;
    }

    public boolean hasNilAttribute() {
        return nilAttribute;
    }

    public int getDepth() {
        return depth;
    }
}
