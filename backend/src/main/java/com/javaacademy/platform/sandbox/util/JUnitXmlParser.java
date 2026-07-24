package com.javaacademy.platform.sandbox.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JUnitXmlParser {

    /**
     * Parses the JUnit Platform Console XML report for the given test class and returns the number
     * of failed + errored tests. Returns empty if the report file does not exist (e.g. compilation
     * failed before any test ran) or cannot be parsed.
     */
    public static OptionalInt parseFailedTestCount(Path reportDir, String testClassName) {
        Path reportFile = reportDir.resolve("TEST-" + testClassName + ".xml");
        if (!Files.exists(reportFile)) {
            return OptionalInt.empty();
        }
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(reportFile.toFile());
            var testsuite = doc.getDocumentElement();
            int failures = intAttr(testsuite.getAttribute("failures"));
            int errors = intAttr(testsuite.getAttribute("errors"));
            return OptionalInt.of(failures + errors);
        } catch (ParserConfigurationException | SAXException | IOException parseException) {
            log.warn("Could not parse JUnit XML report {}", reportFile, parseException);
            return OptionalInt.empty();
        }
    }

    private static int intAttr(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }
}
