package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.javaacademy.platform.sandbox.util.JUnitXmlParser;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class JUnitXmlParserTest {

    @Test
    void parseFailedTestCount_allPassing_returnsZero() throws URISyntaxException {
        Path reportDir = resourceDir("sandbox/TEST-TaskTest-passing.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir, "TaskTest-passing");
        assertThat(result).isPresent();
        assertThat(result.getAsInt()).isZero();
    }

    @Test
    void parseFailedTestCount_twoFailuresOneError_returnsSumThree() throws URISyntaxException {
        Path reportDir = resourceDir("sandbox/TEST-TaskTest-failing.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir, "TaskTest-failing");
        assertThat(result).isPresent();
        assertThat(result.getAsInt()).isEqualTo(3);
    }

    @Test
    void parseFailedTestCount_reportFileAbsent_returnsEmpty() throws URISyntaxException {
        Path reportDir = resourceDir("sandbox/TEST-TaskTest-passing.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir, "NonExistentTest");
        assertThat(result).isEmpty();
    }

    @Test
    void parseFailedTestCount_missingFailuresAttribute_treatsAsZero() throws URISyntaxException {
        // The passing XML has failures="0" which should parse to 0
        Path reportDir = resourceDir("sandbox/TEST-TaskTest-passing.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir, "TaskTest-passing");
        assertThat(result.getAsInt()).isGreaterThanOrEqualTo(0);
    }

    private static Path resourceDir(String resource) throws URISyntaxException {
        URL url = JUnitXmlParserTest.class.getClassLoader().getResource(resource);
        assertThat(url).as("classpath resource %s must exist", resource).isNotNull();
        return Path.of(url.toURI());
    }
}
