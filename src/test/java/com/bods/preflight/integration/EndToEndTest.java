package com.bods.preflight.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bods.preflight.BodsPreflightChecker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EndToEndTest {
    @Test
    void shouldReturnZeroForPassingFixture() {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        int exitCode = checker.run(new String[] {
                "--xml", "fixtures/pairs/pass/sample.xml",
                "--xsd", "fixtures/pairs/pass/sample.xsd"
        });
        assertEquals(0, exitCode);
    }

    @Test
    void shouldReturnOneForRequiredNullFixture() {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        int exitCode = checker.run(new String[] {
                "--xml", "fixtures/pairs/fail-required-null/sample.xml",
                "--xsd", "fixtures/pairs/fail-required-null/sample.xsd"
        });
        assertEquals(1, exitCode);
    }

    @Test
    void shouldReturnTwoForWarningsWhenFailOnWarningIsSet() {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        int exitCode = checker.run(new String[] {
                "--xml", "fixtures/pairs/warn-large-xml/sample.xml",
                "--xsd", "fixtures/pairs/warn-large-xml/sample.xsd",
                "--fail-on-warning"
        });
        assertEquals(2, exitCode);
    }

    @Test
    void shouldReturnOneForRootMismatchFixture() {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        int exitCode = checker.run(new String[] {
                "--xml", "fixtures/pairs/fail-root-mismatch/sample.xml",
                "--xsd", "fixtures/pairs/fail-root-mismatch/sample.xsd"
        });
        assertEquals(1, exitCode);
    }

    @Test
    void shouldRecommendMaxSizeAbove4000ForLargeXml() throws IOException {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        Path output = Files.createTempFile("bods-preflight-large-", ".txt");
        try {
            checker.run(new String[] {
                    "--xml", "fixtures/pairs/warn-large-xml/sample.xml",
                    "--xsd", "fixtures/pairs/warn-large-xml/sample.xsd",
                    "--output", output.toString()
            });
            String report = Files.readString(output);
            // The report must contain the WARNING about exceeding the 4000 default.
            assertTrue(report.contains("XML character count exceeds the BODS default max_size"),
                    "Report must contain max_size warning for large XML");
            // The recommended max_size line must show a value greater than 4000.
            // TextReportWriter renders it as "  max_size             : NNNNN  (...)"
            assertTrue(report.matches("(?s).*max_size\\s*:\\s*[5-9]\\d{3}[^\\d].*")
                    || report.matches("(?s).*max_size\\s*:\\s*\\d{5,}[^\\d].*"),
                    "Recommended max_size must exceed 4000");
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    void shouldWriteHtmlReportToOutputFile() throws IOException {
        BodsPreflightChecker checker = new BodsPreflightChecker();
        Path output = Files.createTempFile("bods-preflight-", ".html");
        try {
            int exitCode = checker.run(new String[] {
                    "--xml", "fixtures/pairs/pass/sample.xml",
                    "--xsd", "fixtures/pairs/pass/sample.xsd",
                    "--format", "html",
                    "--output", output.toString()
            });
            assertEquals(0, exitCode);
            String contents = Files.readString(output);
            assertTrue(contents.contains("<!DOCTYPE html>"));
            assertTrue(contents.contains("SECTION A: XSD ANALYSIS"));
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
