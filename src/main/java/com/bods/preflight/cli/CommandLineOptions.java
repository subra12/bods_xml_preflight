package com.bods.preflight.cli;

import java.nio.file.Path;

public class CommandLineOptions {
    private Path xmlPath;
    private Path xsdPath;
    private Path outputPath;
    private String format = "txt";
    private boolean verbose;
    private boolean failOnWarning;
    private String forcedEncoding;
    private boolean helpRequested;

    public Path getXmlPath() {
        return xmlPath;
    }

    public void setXmlPath(Path xmlPath) {
        this.xmlPath = xmlPath;
    }

    public Path getXsdPath() {
        return xsdPath;
    }

    public void setXsdPath(Path xsdPath) {
        this.xsdPath = xsdPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isFailOnWarning() {
        return failOnWarning;
    }

    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    public String getForcedEncoding() {
        return forcedEncoding;
    }

    public void setForcedEncoding(String forcedEncoding) {
        this.forcedEncoding = forcedEncoding;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public void setHelpRequested(boolean helpRequested) {
        this.helpRequested = helpRequested;
    }
}
