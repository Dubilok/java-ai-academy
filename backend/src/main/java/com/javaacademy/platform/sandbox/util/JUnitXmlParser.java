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
     * Parses the JUnit Platform Console XML report and returns the number of failed + errored
     * tests. The console launcher always writes {@code TEST-junit-jupiter.xml}; returns empty when
     * that file is absent (compilation failed, or JVM terminated before tests ran).
     */
    public static OptionalInt parseFailedTestCount(Path reportDir) {
        Path reportFile = reportDir.resolve("TEST-junit-jupiter.xml");
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
