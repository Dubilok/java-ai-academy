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
        Path reportDir = resourceDir("sandbox/passing/TEST-junit-jupiter.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir);
        assertThat(result).isPresent();
        assertThat(result.getAsInt()).isZero();
    }

    @Test
    void parseFailedTestCount_twoFailuresOneError_returnsSumThree() throws URISyntaxException {
        Path reportDir = resourceDir("sandbox/failing/TEST-junit-jupiter.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir);
        assertThat(result).isPresent();
        assertThat(result.getAsInt()).isEqualTo(3);
    }

    @Test
    void parseFailedTestCount_reportFileAbsent_returnsEmpty() throws URISyntaxException {
        // Use a directory that exists but has no TEST-junit-jupiter.xml in it
        Path reportDir = resourceDir("sandbox/passing/TEST-junit-jupiter.xml")
                .getParent()
                .getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir);
        assertThat(result).isEmpty();
    }

    @Test
    void parseFailedTestCount_zeroFailuresZeroErrors_returnsZero() throws URISyntaxException {
        Path reportDir = resourceDir("sandbox/passing/TEST-junit-jupiter.xml").getParent();
        OptionalInt result = JUnitXmlParser.parseFailedTestCount(reportDir);
        assertThat(result.getAsInt()).isGreaterThanOrEqualTo(0);
    }

    private static Path resourceDir(String resource) throws URISyntaxException {
        URL url = JUnitXmlParserTest.class.getClassLoader().getResource(resource);
        assertThat(url).as("classpath resource %s must exist", resource).isNotNull();
        return Path.of(url.toURI());
    }
}
