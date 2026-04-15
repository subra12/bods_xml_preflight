package com.bods.preflight.report;

import com.bods.preflight.model.DiagnosticReport;
import com.bods.preflight.model.Issue;
import com.bods.preflight.model.ParameterRecommendations;
import com.bods.preflight.model.Severity;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class HtmlReportWriter {
    public String write(DiagnosticReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<title>").append(escape(report.getToolName())).append("</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;margin:24px;line-height:1.45;color:#1f2937;}")
                .append("h1,h2{color:#0f4c81;} table{border-collapse:collapse;width:100%;margin:12px 0;}")
                .append("th,td{border:1px solid #d1d5db;padding:8px;text-align:left;vertical-align:top;}")
                .append(".severity-CRITICAL{color:#b91c1c;font-weight:700;}")
                .append(".severity-ERROR{color:#c2410c;font-weight:700;}")
                .append(".severity-WARNING{color:#a16207;font-weight:700;}")
                .append(".severity-INFO{color:#1d4ed8;}")
                .append(".status{font-size:1.1rem;font-weight:700;padding:12px;border:1px solid #cbd5e1;background:#f8fafc;}")
                .append("</style></head><body>");

        builder.append("<h1>").append(escape(report.getToolName())).append(" ").append(escape(report.getVersion())).append("</h1>");
        builder.append("<p><strong>Timestamp:</strong> ")
                .append(escape(report.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append("<br><strong>XML File:</strong> ").append(escape(report.getXmlFile().toString()))
                .append("<br><strong>XSD File:</strong> ").append(escape(report.getXsdFile().toString()))
                .append("</p>");

        builder.append(section("SECTION A: XSD ANALYSIS",
                table(new String[][] {
                        {"Root Element", value(report.getXsdAnalysis().getRootElementName())},
                        {"Single Child Element", value(report.getXsdAnalysis().getSingleChildElementName())
                                + (report.getXsdAnalysis().getSingleChildElementName() == null ? ""
                                : " (maxOccurs=" + value(report.getXsdAnalysis().getSingleChildMaxOccurs()) + ")")},
                        {"Total Elements", Integer.toString(report.getXsdAnalysis().getTotalElements())},
                        {"Required Elements", Integer.toString(report.getXsdAnalysis().getTotalRequiredElements())},
                        {"Optional Elements", Integer.toString(report.getXsdAnalysis().getTotalOptionalElements())},
                        {"Max Nesting Depth", Integer.toString(report.getXsdAnalysis().getMaxNestingDepth())}
                })));

        builder.append(section("SECTION B: XML ANALYSIS",
                table(new String[][] {
                        {"Root Element", value(report.getXmlAnalysis().getRootElementName())},
                        {"File Size", report.getXmlAnalysis().getFileSizeBytes() + " bytes (" + String.format("%.1f", report.getXmlAnalysis().getFileSizeKb()) + " KB)"},
                        {"Character Count", report.getXmlAnalysis().getCharacterCount() + " characters"},
                        {"Encoding", value(report.getXmlAnalysis().getEncoding())},
                        {"NULL Elements Found", Integer.toString(report.getXmlAnalysis().getTotalNullElements())},
                        {"Empty Elements Found", Integer.toString(report.getXmlAnalysis().getTotalEmptyElements())}
                }) + issuesList(report.getXmlAnalysis().getIssues())));

        builder.append(section("SECTION C: CROSS-VALIDATION",
                table(new String[][] {
                        {"Root Match", value(report.getValidationResult().getRootMatchExplanation())},
                        {"Required Fields", report.getValidationResult().getRequiredNullElements().size() + " CRITICAL issue(s) found"},
                        {"Unknown Elements", statusFromList(report.getValidationResult().getUnknownXmlElements())},
                        {"Data Types", statusFromList(report.getValidationResult().getTypeMismatchElements())},
                        {"Namespace", report.getValidationResult().isNamespaceMatch() ? "PASS" : "WARNING"}
                }) + issuesList(report.getValidationResult().getIssues())));

        ParameterRecommendations recommendations = report.getParameterRecommendations();
        builder.append(section("SECTION D: BODS PARAMETER RECOMMENDATIONS",
                table(new String[][] {
                        {"nested_table_name", value(recommendations.getNestedTableName())},
                        {"schema_dtd_name", value(recommendations.getSchemaDtdName())},
                        {"is_top_level_element", value(recommendations.getIsTopLevelElement()) + " (" + value(recommendations.getIsTopLevelElementExplanation()) + ")"},
                        {"max_size", recommendations.getRecommendedMaxSize() + " (" + value(recommendations.getMaxSizeExplanation()) + ")"},
                        {"xml_header", value(recommendations.getXmlHeader()) + " (" + value(recommendations.getXmlHeaderExplanation()) + ")"},
                        {"replace_null_string", value(recommendations.getReplaceNullString())},
                        {"enable_validation", value(recommendations.getEnableValidation()) + " (" + value(recommendations.getEnableValidationExplanation()) + ")"}
                })));

        Map<Severity, Integer> counts = report.countBySeverity();
        builder.append(section("SECTION E: ISSUES SUMMARY",
                table(new String[][] {
                        {"CRITICAL", Integer.toString(counts.get(Severity.CRITICAL))},
                        {"ERROR", Integer.toString(counts.get(Severity.ERROR))},
                        {"WARNING", Integer.toString(counts.get(Severity.WARNING))},
                        {"INFO", Integer.toString(counts.get(Severity.INFO))}
                })));

        builder.append(section("SECTION F: PRE-FLIGHT STATUS",
                "<div class=\"status\">" + escape(report.getOverallStatus()) + "</div>"));
        builder.append("</body></html>");
        return builder.toString();
    }

    private String section(String title, String body) {
        return "<h2>" + escape(title) + "</h2>" + body;
    }

    private String table(String[][] rows) {
        StringBuilder builder = new StringBuilder("<table>");
        for (String[] row : rows) {
            builder.append("<tr><th>").append(escape(row[0])).append("</th><td>")
                    .append(escape(row[1])).append("</td></tr>");
        }
        builder.append("</table>");
        return builder.toString();
    }

    private String issuesList(List<Issue> issues) {
        if (issues.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("<table><tr><th>Severity</th><th>Message</th><th>XPath</th><th>Recommendation</th></tr>");
        for (Issue issue : issues) {
            builder.append("<tr><td class=\"severity-").append(issue.getSeverity()).append("\">")
                    .append(issue.getSeverity()).append("</td><td>").append(escape(issue.getMessage()))
                    .append("</td><td>").append(escape(value(issue.getXPath())))
                    .append("</td><td>").append(escape(value(issue.getRecommendation())))
                    .append("</td></tr>");
        }
        builder.append("</table>");
        return builder.toString();
    }

    private String statusFromList(List<String> values) {
        return values.isEmpty() ? "PASS" : values.size() + " issue(s) found";
    }

    private String value(String input) {
        return input == null || input.isBlank() ? "N/A" : input;
    }

    private String escape(String input) {
        String value = value(input);
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
