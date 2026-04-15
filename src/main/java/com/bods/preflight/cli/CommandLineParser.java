package com.bods.preflight.cli;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandLineParser {
    public CommandLineOptions parse(String[] args) {
        CommandLineOptions options = new CommandLineOptions();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--xml":
                    options.setXmlPath(nextPath(args, ++i, "--xml"));
                    break;
                case "--xsd":
                    options.setXsdPath(nextPath(args, ++i, "--xsd"));
                    break;
                case "--output":
                    options.setOutputPath(nextPath(args, ++i, "--output"));
                    break;
                case "--format":
                    options.setFormat(nextValue(args, ++i, "--format"));
                    break;
                case "--encoding":
                    options.setForcedEncoding(nextValue(args, ++i, "--encoding"));
                    break;
                case "--verbose":
                    options.setVerbose(true);
                    break;
                case "--fail-on-warning":
                    options.setFailOnWarning(true);
                    break;
                case "--help":
                    options.setHelpRequested(true);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        validate(options);
        return options;
    }

    public String usage() {
        return String.join(System.lineSeparator(),
                "Usage:",
                "  java -jar bods-preflight.jar --xml <path> --xsd <path> [OPTIONS]",
                "",
                "Options:",
                "  --output <file_path>   Write report to a file instead of stdout",
                "  --format <txt|html>    Report output format (default: txt)",
                "  --verbose              Include detailed element-level output",
                "  --fail-on-warning      Return exit code 2 for warnings-only runs",
                "  --encoding <charset>   Force a specific XML parsing encoding",
                "  --help                 Show this usage information");
    }

    private void validate(CommandLineOptions options) {
        if (options.isHelpRequested()) {
            return;
        }
        if (options.getXmlPath() == null) {
            throw new IllegalArgumentException("Missing required argument: --xml");
        }
        if (options.getXsdPath() == null) {
            throw new IllegalArgumentException("Missing required argument: --xsd");
        }
        if (!Files.exists(options.getXmlPath())) {
            throw new IllegalArgumentException("XML file not found: " + options.getXmlPath());
        }
        if (!Files.exists(options.getXsdPath())) {
            throw new IllegalArgumentException("XSD file not found: " + options.getXsdPath());
        }
        if (!"txt".equalsIgnoreCase(options.getFormat()) && !"html".equalsIgnoreCase(options.getFormat())) {
            throw new IllegalArgumentException("Invalid format: " + options.getFormat() + ". Expected txt or html.");
        }
        if (options.getForcedEncoding() != null && !Charset.isSupported(options.getForcedEncoding())) {
            throw new IllegalArgumentException("Unsupported charset: " + options.getForcedEncoding());
        }
    }

    private Path nextPath(String[] args, int index, String option) {
        return Paths.get(nextValue(args, index, option));
    }

    private String nextValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }
}
