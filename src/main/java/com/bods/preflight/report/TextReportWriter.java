package com.bods.preflight.report;

import com.bods.preflight.model.DiagnosticReport;
import com.bods.preflight.model.Issue;
import com.bods.preflight.model.ParameterRecommendations;
import com.bods.preflight.model.Severity;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class TextReportWriter {

    private static final int WARNING_CAP = 5;

    public String write(DiagnosticReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("================================================================").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("  ").append(report.getToolName()).append(" ").append(report.getVersion()).append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("  Timestamp : ").append(report.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(System.lineSeparator())
                .append("  XML File  : ").append(report.getXmlFile()).append(System.lineSeparator())
                .append("  XSD File  : ").append(report.getXsdFile()).append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("================================================================").append(System.lineSeparator())
                .append(System.lineSeparator());

        writeQuickLook(builder, report);
        writeXsdSection(builder, report);
        writeXmlSection(builder, report);
        writeCrossValidationSection(builder, report);
        writeRecommendationSection(builder, report.getParameterRecommendations());
        writeIssuesSection(builder, report.countBySeverity());
        writeStatusSection(builder, report);
        return builder.toString();
    }

    private void writeQuickLook(StringBuilder builder, DiagnosticReport report) {
        Map<Severity, Integer> counts = report.countBySeverity();
        int critical = counts.get(Severity.CRITICAL);
        int error    = counts.get(Severity.ERROR);
        int warning  = counts.get(Severity.WARNING);
        int info     = counts.get(Severity.INFO);

        builder.append("----------------------------------------------------------------").append(System.lineSeparator())
                .append("  QUICK LOOK").append(System.lineSeparator())
                .append("  ").append(critical).append(" CRITICAL  |  ")
                .append(error).append(" ERROR  |  ")
                .append(warning).append(" WARNING  |  ")
                .append(info).append(" INFO").append(System.lineSeparator());

        Issue top = topIssue(report);
        if (top != null) {
            builder.append("  Top issue : ").append(top.getMessage());
            if (top.hasLocation()) {
                builder.append(" (line ").append(top.getLine()).append(")");
            }
            builder.append(System.lineSeparator());
        }

        if (critical > 0 || error > 0) {
            builder.append("  Action    : Fix CRITICAL and ERROR issues first, then re-run.").append(System.lineSeparator());
        } else if (warning > 0) {
            builder.append("  Action    : Review WARNINGs before loading to BODS.").append(System.lineSeparator());
        } else {
            builder.append("  Action    : No blocking issues — ready to load.").append(System.lineSeparator());
        }

        builder.append("----------------------------------------------------------------").append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private Issue topIssue(DiagnosticReport report) {
        for (Severity s : new Severity[]{Severity.CRITICAL, Severity.ERROR, Severity.WARNING}) {
            List<Issue> list = report.getIssuesBySeverity(s);
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    private void writeXsdSection(StringBuilder builder, DiagnosticReport report) {
        builder.append("SECTION A: XSD ANALYSIS").append(System.lineSeparator()).append(System.lineSeparator())
                .append("  Root Element        : ").append(value(report.getXsdAnalysis().getRootElementName())).append(System.lineSeparator())
                .append("  Single Child Element: ").append(value(report.getXsdAnalysis().getSingleChildElementName()));
        if (report.getXsdAnalysis().getSingleChildElementName() != null) {
            builder.append(" (maxOccurs=").append(value(report.getXsdAnalysis().getSingleChildMaxOccurs())).append(")");
        }
        builder.append(System.lineSeparator())
                .append("  Total Elements      : ").append(report.getXsdAnalysis().getTotalElements()).append(System.lineSeparator())
                .append("  Required Elements   : ").append(report.getXsdAnalysis().getTotalRequiredElements()).append(System.lineSeparator())
                .append("  Optional Elements   : ").append(report.getXsdAnalysis().getTotalOptionalElements()).append(System.lineSeparator())
                .append("  Max Nesting Depth   : ").append(report.getXsdAnalysis().getMaxNestingDepth()).append(System.lineSeparator());

        List<String> importedPaths = report.getXsdAnalysis().getResolvedImportedSchemaPaths();
        if (!importedPaths.isEmpty()) {
            builder.append("  Imported Schemas    :").append(System.lineSeparator());
            for (String path : importedPaths) {
                builder.append("    + ").append(path).append(System.lineSeparator());
            }
        }

        writeIssueList(builder, report.getXsdAnalysis().getIssues(), report.isVerbose());
        builder.append(System.lineSeparator());
    }

    private void writeXmlSection(StringBuilder builder, DiagnosticReport report) {
        builder.append("SECTION B: XML ANALYSIS").append(System.lineSeparator()).append(System.lineSeparator())
                .append("  Root Element        : ").append(value(report.getXmlAnalysis().getRootElementName())).append(System.lineSeparator())
                .append("  File Size           : ").append(report.getXmlAnalysis().getFileSizeBytes()).append(" bytes (")
                .append(String.format("%.1f", report.getXmlAnalysis().getFileSizeKb())).append(" KB)").append(System.lineSeparator())
                .append("  Character Count     : ").append(report.getXmlAnalysis().getCharacterCount()).append(" characters").append(System.lineSeparator())
                .append("  Encoding            : ").append(value(report.getXmlAnalysis().getEncoding())).append(System.lineSeparator())
                .append("  XML Header          : ").append(value(report.getXmlAnalysis().getXmlDeclaration())).append(System.lineSeparator())
                .append("  NULL Elements Found : ").append(report.getXmlAnalysis().getTotalNullElements()).append(System.lineSeparator())
                .append("  Empty Elements Found: ").append(report.getXmlAnalysis().getTotalEmptyElements()).append(System.lineSeparator());
        writeIssueList(builder, report.getXmlAnalysis().getIssues(), report.isVerbose());
        if (report.isVerbose()) {
            builder.append("  Root Child Count    : ").append(report.getXmlAnalysis().getRootDirectChildCount()).append(System.lineSeparator())
                    .append("  Single Child Type   : ").append(report.getXmlAnalysis().isSingleRepeatingChildType()).append(System.lineSeparator())
                    .append("  Max Nesting Depth   : ").append(report.getXmlAnalysis().getMaxNestingDepth()).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
    }

    private void writeCrossValidationSection(StringBuilder builder, DiagnosticReport report) {
        ParameterRecommendations rec = report.getParameterRecommendations();

        builder.append("SECTION C: CROSS-VALIDATION").append(System.lineSeparator()).append(System.lineSeparator())
                .append("  Root Match          : ").append(report.getValidationResult().getRootMatchExplanation()).append(System.lineSeparator())
                .append("  is_top_level_element: ").append(value(rec.getIsTopLevelElement()))
                .append("  (").append(value(rec.getIsTopLevelElementExplanation())).append(")").append(System.lineSeparator())
                .append("  Required Fields     : ").append(report.getValidationResult().getRequiredNullElements().size()).append(" CRITICAL issue(s) found").append(System.lineSeparator())
                .append("  Unknown Elements    : ").append(statusFromList(report.getValidationResult().getUnknownXmlElements())).append(System.lineSeparator())
                .append("  Data Types          : ").append(statusFromList(report.getValidationResult().getTypeMismatchElements())).append(System.lineSeparator())
                .append("  Namespace           : ").append(report.getValidationResult().isNamespaceMatch() ? "PASS" : "WARNING — see issues below").append(System.lineSeparator());
        writeIssueList(builder, report.getValidationResult().getIssues(), report.isVerbose());
        builder.append(System.lineSeparator());
    }

    private void writeRecommendationSection(StringBuilder builder, ParameterRecommendations recommendations) {
        builder.append("SECTION D: BODS PARAMETER RECOMMENDATIONS").append(System.lineSeparator()).append(System.lineSeparator())
                .append("  nested_table_name    : ").append(value(recommendations.getNestedTableName())).append(System.lineSeparator())
                .append("  schema_dtd_name      : ").append(value(recommendations.getSchemaDtdName())).append(System.lineSeparator())
                .append("  is_top_level_element : ").append(value(recommendations.getIsTopLevelElement()))
                .append("  (").append(value(recommendations.getIsTopLevelElementExplanation())).append(")").append(System.lineSeparator())
                .append("  max_size             : ").append(recommendations.getRecommendedMaxSize())
                .append("  (").append(value(recommendations.getMaxSizeExplanation())).append(")").append(System.lineSeparator())
                .append("  xml_header           : ").append(value(recommendations.getXmlHeader()))
                .append("  (").append(value(recommendations.getXmlHeaderExplanation())).append(")").append(System.lineSeparator())
                .append("  replace_null_string  : ").append(value(recommendations.getReplaceNullString()));
        if (!recommendations.getReplaceNullStringAffectedPaths().isEmpty()) {
            builder.append("  (affected: ").append(String.join(", ", recommendations.getReplaceNullStringAffectedPaths())).append(")");
        }
        builder.append(System.lineSeparator())
                .append("  enable_validation    : ").append(value(recommendations.getEnableValidation()))
                .append("  (").append(value(recommendations.getEnableValidationExplanation())).append(")").append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private void writeIssuesSection(StringBuilder builder, Map<Severity, Integer> counts) {
        builder.append("SECTION E: ISSUES SUMMARY").append(System.lineSeparator()).append(System.lineSeparator())
                .append("  CRITICAL : ").append(counts.get(Severity.CRITICAL)).append(System.lineSeparator())
                .append("  ERROR    : ").append(counts.get(Severity.ERROR)).append(System.lineSeparator())
                .append("  WARNING  : ").append(counts.get(Severity.WARNING)).append(System.lineSeparator())
                .append("  INFO     : ").append(counts.get(Severity.INFO)).append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private void writeStatusSection(StringBuilder builder, DiagnosticReport report) {
        builder.append("SECTION F: PRE-FLIGHT STATUS").append(System.lineSeparator()).append(System.lineSeparator())
                .append("  *** ").append(report.getOverallStatus());
        if ("FAIL".equals(report.getOverallStatus())) {
            builder.append(" - Resolve critical or error issues before running in BODS");
        } else if ("PASS WITH WARNINGS".equals(report.getOverallStatus())) {
            builder.append(" - Review warnings before running in BODS");
        } else {
            builder.append(" - No blocking issues found");
        }
        builder.append(" ***").append(System.lineSeparator()).append(System.lineSeparator())
                .append("================================================================").append(System.lineSeparator());
    }

    /**
     * Renders issues with three display rules:
     *   CRITICAL / ERROR — always printed in full, no limit.
     *   WARNING          — first WARNING_CAP printed; remainder summarised unless verbose.
     *   INFO             — count-only line unless verbose.
     */
    private void writeIssueList(StringBuilder builder, List<Issue> issues, boolean verbose) {
        int warningCount = 0;
        int infoCount = 0;
        for (Issue i : issues) {
            if (i.getSeverity() == Severity.WARNING) warningCount++;
            if (i.getSeverity() == Severity.INFO)    infoCount++;
        }

        int warningsRendered = 0;
        for (Issue issue : issues) {
            Severity sev = issue.getSeverity();
            if (sev == Severity.INFO) {
                if (!verbose) continue;
            }
            if (sev == Severity.WARNING && !verbose && warningsRendered >= WARNING_CAP) {
                continue;
            }
            if (sev == Severity.WARNING) warningsRendered++;

            builder.append("    [").append(sev).append("] ");
            if (issue.hasLocation()) {
                builder.append("line ").append(issue.getLine())
                        .append(" col ").append(issue.getColumn()).append("  ");
            }
            builder.append(value(issue.getXPath())).append("  ").append(issue.getMessage());
            if (issue.getRecommendation() != null && !issue.getRecommendation().isBlank()) {
                builder.append(" Recommendation: ").append(issue.getRecommendation());
            }
            builder.append(System.lineSeparator());
        }

        if (!verbose) {
            int hidden = warningCount - warningsRendered;
            if (hidden > 0) {
                builder.append("    ... and ").append(hidden)
                        .append(" more WARNING(s) not shown. Run with --verbose to see all.")
                        .append(System.lineSeparator());
            }
            if (infoCount > 0) {
                builder.append("    [INFO x").append(infoCount)
                        .append("] Run with --verbose to see details.")
                        .append(System.lineSeparator());
            }
        }
    }

    private String statusFromList(List<String> values) {
        return values.isEmpty() ? "PASS" : values.size() + " issue(s) found";
    }

    private String value(String input) {
        return input == null || input.isBlank() ? "N/A" : input;
    }
}
