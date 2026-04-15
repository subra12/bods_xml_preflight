package com.bods.preflight.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public final class XmlUtils {
    private static final Pattern XML_DECL_PATTERN =
            Pattern.compile("^\\s*(<\\?xml\\s+version=.*?\\?>)", Pattern.DOTALL);
    private static final Pattern XML_ENCODING_PATTERN =
            Pattern.compile("encoding\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    private XmlUtils() {
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public static String extractXmlDeclaration(byte[] bytes) {
        String prefix = new String(bytes, 0, Math.min(bytes.length, 300), StandardCharsets.UTF_8);
        Matcher matcher = XML_DECL_PATTERN.matcher(prefix);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String detectEncoding(String xmlDeclaration, String forcedEncoding) {
        if (forcedEncoding != null && !forcedEncoding.isBlank()) {
            return forcedEncoding;
        }
        if (xmlDeclaration != null) {
            Matcher matcher = XML_ENCODING_PATTERN.matcher(xmlDeclaration);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return StandardCharsets.UTF_8.name();
    }

    public static String decode(byte[] bytes, String encoding) {
        return new String(bytes, Charset.forName(encoding));
    }

    public static Document parseXml(byte[] bytes, String encoding) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(decode(bytes, encoding).getBytes(Charset.forName(encoding))));
    }
}
