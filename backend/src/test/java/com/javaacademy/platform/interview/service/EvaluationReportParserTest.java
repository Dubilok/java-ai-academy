package com.javaacademy.platform.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluationReportParserTest {

    static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final String TECHNOLOGY = "Java";
    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-25T12:00:00Z"), ZoneOffset.UTC);

    EvaluationReportParser parser;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new EvaluationReportParser(new ObjectMapper(), validator, FIXED_CLOCK);
    }

    // ── happy path ─────────────────────────────────────────────────────────────

    @Test
    void parse_validJson_returnsReportWithCorrectDimensionCount() {
        EvaluationReport report = parser.parse(validEvaluationJson(), SESSION_ID, TECHNOLOGY);

        assertThat(report.dimensions()).hasSize(10);
        assertThat(report.sessionId()).isEqualTo(SESSION_ID);
        assertThat(report.technology()).isEqualTo(TECHNOLOGY);
    }

    @Test
    void parse_validJson_overallScoreIsAverageOfDimensions() {
        EvaluationReport report = parser.parse(validEvaluationJson(), SESSION_ID, TECHNOLOGY);

        int expectedAverage = (int) Math.round(
                report.dimensions().stream().mapToInt(d -> d.score()).average().orElse(0));
        assertThat(report.overallScore()).isEqualTo(expectedAverage);
    }

    @Test
    void parse_validJson_setsCompletedAtFromClock() {
        EvaluationReport report = parser.parse(validEvaluationJson(), SESSION_ID, TECHNOLOGY);

        assertThat(report.completedAt()).isEqualTo(Instant.parse("2026-07-25T12:00:00Z"));
    }

    @Test
    void parse_validJson_firstDimensionHasCorrectFields() {
        EvaluationReport report = parser.parse(validEvaluationJson(), SESSION_ID, TECHNOLOGY);

        assertThat(report.dimensions().get(0).name()).isEqualTo("technicalAccuracy");
        assertThat(report.dimensions().get(0).score()).isEqualTo(80);
        assertThat(report.dimensions().get(0).feedback()).isNotBlank();
    }

    // ── validation failures ────────────────────────────────────────────────────

    @Test
    void parse_invalidJson_throwsLlmException() {
        assertThatThrownBy(() -> parser.parse("not valid json", SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Failed to parse evaluation JSON");
    }

    @Test
    void parse_missingDimensionsArray_throwsLlmException() {
        assertThatThrownBy(() -> parser.parse("{\"other\": \"data\"}", SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("missing 'dimensions' array");
    }

    @Test
    void parse_fewerThan10Dimensions_throwsLlmException() {
        String json = fewDimensionsJson(5);
        assertThatThrownBy(() -> parser.parse(json, SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("exactly 10 dimensions");
    }

    @Test
    void parse_moreThan10Dimensions_throwsLlmException() {
        String json = fewDimensionsJson(11);
        assertThatThrownBy(() -> parser.parse(json, SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("exactly 10 dimensions");
    }

    @Test
    void parse_dimensionScoreAbove100_throwsLlmException() {
        String json = jsonWithOneInvalidDimension(150);
        assertThatThrownBy(() -> parser.parse(json, SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("failed validation");
    }

    @Test
    void parse_dimensionScoreBelow0_throwsLlmException() {
        String json = jsonWithOneInvalidDimension(-5);
        assertThatThrownBy(() -> parser.parse(json, SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("failed validation");
    }

    @Test
    void parse_blankDimensionFeedback_throwsLlmException() {
        String json = jsonWithBlankFeedback();
        assertThatThrownBy(() -> parser.parse(json, SESSION_ID, TECHNOLOGY))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("failed validation");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static String validEvaluationJson() {
        return """
                {
                  "dimensions": [
                    { "name": "technicalAccuracy",      "score": 80, "feedback": "Solid understanding of core Java." },
                    { "name": "depthOfKnowledge",        "score": 70, "feedback": "Good depth on most topics." },
                    { "name": "practicalApplication",    "score": 75, "feedback": "Applied concepts well to examples." },
                    { "name": "communicationClarity",    "score": 85, "feedback": "Explained ideas clearly." },
                    { "name": "breadthOfCoverage",       "score": 60, "feedback": "Missed some related aspects." },
                    { "name": "exampleQuality",          "score": 72, "feedback": "Examples were mostly relevant." },
                    { "name": "problemSolvingApproach",  "score": 78, "feedback": "Logical approach to problems." },
                    { "name": "edgeCaseAwareness",       "score": 55, "feedback": "Edge cases were underexplored." },
                    { "name": "modernJavaAwareness",     "score": 65, "feedback": "Familiar with Java 8 features." },
                    { "name": "learningPotential",       "score": 82, "feedback": "Showed curiosity and adaptability." }
                  ]
                }
                """;
    }

    private static String fewDimensionsJson(int count) {
        StringBuilder sb = new StringBuilder("{\"dimensions\":[");
        for (int idx = 0; idx < count; idx++) {
            if (idx > 0) sb.append(",");
            sb.append("{\"name\":\"dim").append(idx).append("\",\"score\":50,\"feedback\":\"Good.\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonWithOneInvalidDimension(int badScore) {
        return """
                {
                  "dimensions": [
                    { "name": "technicalAccuracy",      "score": %d,  "feedback": "Solid." },
                    { "name": "depthOfKnowledge",        "score": 70, "feedback": "Good." },
                    { "name": "practicalApplication",    "score": 75, "feedback": "Good." },
                    { "name": "communicationClarity",    "score": 85, "feedback": "Clear." },
                    { "name": "breadthOfCoverage",       "score": 60, "feedback": "Ok." },
                    { "name": "exampleQuality",          "score": 72, "feedback": "Good." },
                    { "name": "problemSolvingApproach",  "score": 78, "feedback": "Good." },
                    { "name": "edgeCaseAwareness",       "score": 55, "feedback": "Needs work." },
                    { "name": "modernJavaAwareness",     "score": 65, "feedback": "Ok." },
                    { "name": "learningPotential",       "score": 82, "feedback": "Great." }
                  ]
                }
                """
                .formatted(badScore);
    }

    private static String jsonWithBlankFeedback() {
        return """
                {
                  "dimensions": [
                    { "name": "technicalAccuracy",      "score": 80, "feedback": "" },
                    { "name": "depthOfKnowledge",        "score": 70, "feedback": "Good." },
                    { "name": "practicalApplication",    "score": 75, "feedback": "Good." },
                    { "name": "communicationClarity",    "score": 85, "feedback": "Clear." },
                    { "name": "breadthOfCoverage",       "score": 60, "feedback": "Ok." },
                    { "name": "exampleQuality",          "score": 72, "feedback": "Good." },
                    { "name": "problemSolvingApproach",  "score": 78, "feedback": "Good." },
                    { "name": "edgeCaseAwareness",       "score": 55, "feedback": "Needs work." },
                    { "name": "modernJavaAwareness",     "score": 65, "feedback": "Ok." },
                    { "name": "learningPotential",       "score": 82, "feedback": "Great." }
                  ]
                }
                """;
    }
}
