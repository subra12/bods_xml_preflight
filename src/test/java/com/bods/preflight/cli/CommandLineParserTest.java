package com.bods.preflight.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandLineParserTest {
    private final CommandLineParser parser = new CommandLineParser();

    @Test
    void shouldParseRequiredArguments() {
        CommandLineOptions options = parser.parse(new String[] {
                "--xml", "fixtures/pairs/pass/sample.xml",
                "--xsd", "fixtures/pairs/pass/sample.xsd"
        });

        assertTrue(options.getXmlPath().toString().endsWith("fixtures\\pairs\\pass\\sample.xml")
                || options.getXmlPath().toString().endsWith("fixtures/pairs/pass/sample.xml"));
        assertEquals("txt", options.getFormat());
    }

    @Test
    void shouldRejectMissingXmlArgument() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"--xsd", "fixtures/pairs/pass/sample.xsd"}));
        assertTrue(exception.getMessage().contains("--xml"));
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {
                        "--xml", "fixtures/pairs/pass/sample.xml",
                        "--xsd", "fixtures/pairs/pass/sample.xsd",
                        "--format", "pdf"
                }));
    }
}
