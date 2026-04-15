package com.bods.preflight.model;

public class ElementDefinition {
    private final String name;
    private final String xPath;
    private final String dataType;
    private final int minOccurs;
    private final String maxOccurs;
    private final boolean nillable;
    private final boolean required;
    private final boolean complexType;
    private final int depth;

    public ElementDefinition(String name, String xPath, String dataType, int minOccurs, String maxOccurs,
            boolean nillable, boolean required, boolean complexType, int depth) {
        this.name = name;
        this.xPath = xPath;
        this.dataType = dataType;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.nillable = nillable;
        this.required = required;
        this.complexType = complexType;
        this.depth = depth;
    }

    public String getName() {
        return name;
    }

    public String getXPath() {
        return xPath;
    }

    public String getDataType() {
        return dataType;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public String getMaxOccurs() {
        return maxOccurs;
    }

    public boolean isNillable() {
        return nillable;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isComplexType() {
        return complexType;
    }

    public int getDepth() {
        return depth;
    }
}
