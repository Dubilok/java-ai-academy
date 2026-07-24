package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.AnthropicProperties;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.repository.AiGenerationLogRepository;
import com.javaacademy.platform.catalog.enums.Difficulty;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import jakarta.validation.Validation;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for ContentArchitectService that exercises the real ContentParser
 * (Jackson deserialization + Jakarta Validation) against a hardcoded, realistic LLM
 * response fixture. No live API calls — LlmClient is stubbed, CodeExecutionEngine is stubbed.
 */
class ContentArchitectServiceIntegrationTest {

    static final AnthropicProperties TEST_PROPS = new AnthropicProperties(
            "key", "https://api.anthropic.com", "2023-06-01", "claude-test", 3, 1000L, 3.0, 15.0);

    /** Recorded/golden LLM response matching the GeneratedContent JSON schema. */
    static final String RECORDED_LLM_RESPONSE =
            """
            {
              "lecture": {
                "title": "Java Records: Immutable Data Classes",
                "contentMarkdown": "## Introduction\\n\\nJava Records, introduced in Java 16 as a standard feature, are a special kind of class in Java that acts as a transparent carrier for immutable data. Unlike traditional classes, records require significantly less boilerplate code for common patterns.\\n\\n## Why Records?\\n\\nBefore records, creating a simple data-holding class required: a constructor, getters for each field, equals(), hashCode(), and toString() methods. A record declares all of this automatically from the record components declared in the header.\\n\\n## Basic Syntax\\n\\n```java\\npublic record Point(double x, double y) {}\\n```\\n\\nThe above declaration automatically generates: a canonical constructor, accessor methods x() and y(), equals(), hashCode(), and toString(). Records are implicitly final and cannot extend other classes. They can implement interfaces.\\n\\n## Custom Methods\\n\\nYou can add custom methods to records. For example, a distanceTo method on Point:\\n\\n```java\\npublic double distanceTo(Point other) {\\n    double dx = this.x - other.x;\\n    double dy = this.y - other.y;\\n    return Math.sqrt(dx * dx + dy * dy);\\n}\\n```\\n\\n## Key Properties\\n\\nRecord components are private and final. The canonical constructor sets all components. You may add compact constructors for validation. Records are a good fit for DTOs, value objects, and configuration data."
              },
              "task": {
                "title": "Implement a distanceTo method on a Point record",
                "description": "Create a Java record named Solution that wraps a Point record and provides a static method computeDistance(double x1, double y1, double x2, double y2) returning the Euclidean distance between the two points.",
                "difficulty": "EASY",
                "xpReward": 75
              },
              "templateCode": "public class Solution {\\n    // TODO: implement computeDistance(double x1, double y1, double x2, double y2)\\n    public static double computeDistance(double x1, double y1, double x2, double y2) {\\n        return 0.0;\\n    }\\n}",
              "solutionCode": "public class Solution {\\n    public static double computeDistance(double x1, double y1, double x2, double y2) {\\n        double dx = x1 - x2;\\n        double dy = y1 - y2;\\n        return Math.sqrt(dx * dx + dy * dy);\\n    }\\n}",
              "testCode": "import org.junit.jupiter.api.Test;\\nimport static org.junit.jupiter.api.Assertions.*;\\npublic class TaskTest {\\n    @Test void originToPoint_returnsCorrectDistance() {\\n        assertEquals(5.0, Solution.computeDistance(0, 0, 3, 4), 1e-9);\\n    }\\n    @Test void samePoint_returnsZero() {\\n        assertEquals(0.0, Solution.computeDistance(1, 1, 1, 1), 1e-9);\\n    }\\n    @Test void negativeCoords_returnsPositiveDistance() {\\n        assertEquals(Math.sqrt(2.0), Solution.computeDistance(-1, -1, 0, 0), 1e-9);\\n    }\\n}"
            }
            """;

    LlmClient llmClient;
    CodeExecutionEngine executionEngine;
    AiGenerationLogRepository generationLogRepository;
    ContentArchitectService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        executionEngine = mock(CodeExecutionEngine.class);
        generationLogRepository = mock(AiGenerationLogRepository.class);

        // Real ContentParser — exercises full Jackson + Validator path
        ObjectMapper objectMapper = new ObjectMapper();
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        ContentParser realContentParser = new ContentParser(objectMapper, validator);

        service = new ContentArchitectService(
                llmClient,
                realContentParser,
                executionEngine,
                generationLogRepository,
                TEST_PROPS,
                Clock.systemUTC(),
                "test-system-prompt");
    }

    @Test
    void generateForTopic_withRecordedLlmResponse_parsesAndReturnsContent() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(RECORDED_LLM_RESPONSE, 800, 400));
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.passed("All tests pass", 1200L));

        GeneratedContent result = service.generateForTopic("Java Records");

        assertThat(result).isNotNull();
        assertThat(result.lecture().title()).isEqualTo("Java Records: Immutable Data Classes");
        assertThat(result.lecture().contentMarkdown()).contains("immutable data");
        assertThat(result.task().title()).contains("Point record");
        assertThat(result.task().difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(result.task().xpReward()).isEqualTo(75);
        assertThat(result.templateCode()).contains("computeDistance");
        assertThat(result.solutionCode()).contains("Math.sqrt");
        assertThat(result.testCode()).contains("@Test");
    }

    @Test
    void generateForTopic_withRecordedResponse_selfHealingTriggeredOnFirstSandboxFailure() {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(RECORDED_LLM_RESPONSE, 800, 400));
        when(executionEngine.execute(any()))
                .thenReturn(ExecutionResult.failed(1, "AssertionError: expected 5.0 but was 0.0", 300L))
                .thenReturn(ExecutionResult.passed("All tests pass", 900L));

        GeneratedContent result = service.generateForTopic("Java Records");

        assertThat(result).isNotNull();
        // The second LLM call returns the same valid JSON; parser must succeed both times
        assertThat(result.task().difficulty()).isEqualTo(Difficulty.EASY);
    }
}
