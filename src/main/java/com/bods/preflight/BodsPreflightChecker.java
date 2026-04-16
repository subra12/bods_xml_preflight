package com.bods.preflight;

import com.bods.preflight.analyzer.CrossValidator;
import com.bods.preflight.analyzer.ParameterRecommender;
import com.bods.preflight.analyzer.XmlAnalyzer;
import com.bods.preflight.analyzer.XsdAnalyzer;
import com.bods.preflight.cli.CommandLineOptions;
import com.bods.preflight.cli.CommandLineParser;
import com.bods.preflight.model.DiagnosticReport;
import com.bods.preflight.model.Severity;
import com.bods.preflight.report.HtmlReportWriter;
import com.bods.preflight.report.TextReportWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.xml.sax.SAXParseException;

public class BodsPreflightChecker {
    public static void main(String[] args) {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        int exitCode = checker.run(args);
        System.exit(exitCode);
    }

    public int run(String[] args) {
        CommandLineParser parser = new CommandLineParser();
        try {
            CommandLineOptions options = parser.parse(args);
            if (options.isHelpRequested()) {
                System.out.println(parser.usage());
                return 0;
            }

            DiagnosticReport report = new DiagnosticReport(options.getXmlPath(), options.getXsdPath(), options.isVerbose());
            XsdAnalyzer xsdAnalyzer = new XsdAnalyzer();
            XmlAnalyzer xmlAnalyzer = new XmlAnalyzer();
            CrossValidator validator = new CrossValidator();
            ParameterRecommender recommender = new ParameterRecommender();

            report.setXsdAnalysis(xsdAnalyzer.analyze(options.getXsdPath()));
            report.setXmlAnalysis(xmlAnalyzer.analyze(options.getXmlPath(), options.getForcedEncoding()));
            report.setValidationResult(validator.validate(report.getXsdAnalysis(), report.getXmlAnalysis(),
                    options.getXmlPath(), options.getXsdPath()));
            report.setParameterRecommendations(recommender.recommend(report));

            String output = buildOutput(report, options.getFormat());
            if (options.getOutputPath() != null) {
                Path parent = options.getOutputPath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(options.getOutputPath(), output);
            } else {
                System.out.println(output);
            }
            return determineExitCode(report, options.isFailOnWarning());
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.err.println(parser.usage());
            return 3;
        } catch (Exception exception) {
            System.err.println(buildFriendlyErrorMessage(exception));
            return 3;
        }
    }

    int determineExitCode(DiagnosticReport report, boolean failOnWarning) {
        boolean hasCriticalOrError = report.getAllIssues().stream()
                .anyMatch(issue -> issue.getSeverity() == Severity.CRITICAL || issue.getSeverity() == Severity.ERROR);
        if (hasCriticalOrError) {
            return 1;
        }
        boolean hasWarnings = report.getAllIssues().stream().anyMatch(issue -> issue.getSeverity() == Severity.WARNING);
        if (hasWarnings && failOnWarning) {
            return 2;
        }
        return 0;
    }

    private String buildFriendlyErrorMessage(Exception exception) {
        if (exception instanceof SAXParseException) {
            SAXParseException spe = (SAXParseException) exception;
            return "Tool execution error: XML parse failure at line " + spe.getLineNumber()
                    + ", column " + spe.getColumnNumber() + " — " + spe.getMessage();
        }
        if (exception instanceof IOException) {
            return "Tool execution error: unable to read or write one of the specified files.";
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Tool execution error: an unexpected problem occurred while analyzing the XML and XSD files.";
        }
        return "Tool execution error: " + message;
    }

    private String buildOutput(DiagnosticReport report, String format) {
        if ("html".equalsIgnoreCase(format)) {
            return new HtmlReportWriter().write(report);
        }
        return new TextReportWriter().write(report);
    }
}
