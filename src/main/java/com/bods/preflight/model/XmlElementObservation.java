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
    private final String namespaceUri;
    private final int line;
    private final int column;

    public XmlElementObservation(String name, String xPath, String textValue, boolean hasTextContent,
            boolean hasChildElements, boolean empty, boolean nilAttribute, int depth,
            String namespaceUri, int line, int column) {
        this.name = name;
        this.xPath = xPath;
        this.textValue = textValue;
        this.hasTextContent = hasTextContent;
        this.hasChildElements = hasChildElements;
        this.empty = empty;
        this.nilAttribute = nilAttribute;
        this.depth = depth;
        this.namespaceUri = namespaceUri;
        this.line = line;
        this.column = column;
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

    public String getNamespaceUri() {
        return namespaceUri;
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
