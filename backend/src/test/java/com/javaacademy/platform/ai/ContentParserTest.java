package com.javaacademy.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.service.ContentParser;
import com.javaacademy.platform.catalog.enums.Difficulty;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentParserTest {

    static final String LONG_MARKDOWN = "A".repeat(200); // 200 chars > minimum 100

    static final String LONG_DESCRIPTION = "B".repeat(60); // 60 chars > minimum 30

    static final String VALID_JSON =
            """
            {
              "lecture": {
                "title": "Introduction to Java Records",
                "contentMarkdown": "%s"
              },
              "task": {
                "title": "Create a Point record",
                "description": "%s",
                "difficulty": "EASY",
                "xpReward": 100
              },
              "templateCode": "public class Solution {}",
              "solutionCode": "public class Solution { public int solve() { return 42; } }",
              "testCode": "@Test public void test() { assertEquals(42, new Solution().solve()); }"
            }
            """
                    .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

    ContentParser parser;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new ContentParser(new ObjectMapper(), validator);
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void parse_validJson_returnsGeneratedContent() {
        GeneratedContent content = parser.parse(VALID_JSON);

        assertThat(content.lecture().title()).isEqualTo("Introduction to Java Records");
        assertThat(content.task().difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(content.task().xpReward()).isEqualTo(100);
        assertThat(content.templateCode()).isEqualTo("public class Solution {}");
    }

    // ── strict parsing — unknown fields ──────────────────────────────────────

    @Test
    void parse_unknownField_throwsLlmException() {
        String jsonWithExtra =
                """
                {
                  "lecture": {"title": "L", "contentMarkdown": "%s"},
                  "task": {"title": "T", "description": "%s", "difficulty": "EASY", "xpReward": 50},
                  "templateCode": "code",
                  "solutionCode": "sol",
                  "testCode": "@Test void t() {}",
                  "unexpectedField": "should fail"
                }
                """
                        .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(jsonWithExtra)).isInstanceOf(LlmException.class);
    }

    // ── field-level constraints ───────────────────────────────────────────────

    @Test
    void parse_blankLectureTitle_throwsLlmException() {
        String json =
                """
                {
                  "lecture": {"title": "", "contentMarkdown": "%s"},
                  "task": {"title": "T", "description": "%s", "difficulty": "EASY", "xpReward": 50},
                  "templateCode": "code",
                  "solutionCode": "sol",
                  "testCode": "@Test void t() {}"
                }
                """
                        .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LlmException.class);
    }

    @Test
    void parse_lectureContentTooShort_throwsLlmException() {
        String json =
                """
                {
                  "lecture": {"title": "L", "contentMarkdown": "too short"},
                  "task": {"title": "T", "description": "%s", "difficulty": "EASY", "xpReward": 50},
                  "templateCode": "code",
                  "solutionCode": "sol",
                  "testCode": "@Test void t() {}"
                }
                """
                        .formatted(LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LlmException.class);
    }

    @Test
    void parse_invalidDifficulty_throwsLlmException() {
        String json =
                """
                {
                  "lecture": {"title": "L", "contentMarkdown": "%s"},
                  "task": {"title": "T", "description": "%s", "difficulty": "IMPOSSIBLE", "xpReward": 50},
                  "templateCode": "code",
                  "solutionCode": "sol",
                  "testCode": "@Test void t() {}"
                }
                """
                        .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LlmException.class);
    }

    @Test
    void parse_xpRewardOutOfRange_throwsLlmException() {
        String json =
                """
                {
                  "lecture": {"title": "L", "contentMarkdown": "%s"},
                  "task": {"title": "T", "description": "%s", "difficulty": "EASY", "xpReward": 9999},
                  "templateCode": "code",
                  "solutionCode": "sol",
                  "testCode": "@Test void t() {}"
                }
                """
                        .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LlmException.class);
    }

    @Test
    void parse_missingRequiredField_throwsLlmException() {
        String json =
                """
                {
                  "lecture": {"title": "L", "contentMarkdown": "%s"},
                  "task": {"title": "T", "description": "%s", "difficulty": "EASY", "xpReward": 50},
                  "templateCode": "code",
                  "solutionCode": "sol"
                }
                """
                        .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LlmException.class);
    }

    // ── semantic checks ────────────────────────────────────────────────────────

    @Test
    void parse_testCodeWithoutAtTestAnnotation_throwsLlmException() {
        String json =
                """
                {
                  "lecture": {"title": "L", "contentMarkdown": "%s"},
                  "task": {"title": "T", "description": "%s", "difficulty": "MEDIUM", "xpReward": 50},
                  "templateCode": "code",
                  "solutionCode": "sol",
                  "testCode": "public class TaskTest { void noAnnotation() {} }"
                }
                """
                        .formatted(LONG_MARKDOWN, LONG_DESCRIPTION);

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("@Test");
    }
}
