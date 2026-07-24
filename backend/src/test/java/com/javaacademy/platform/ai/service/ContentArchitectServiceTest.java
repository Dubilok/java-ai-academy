package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.dto.GeneratedLecture;
import com.javaacademy.platform.ai.dto.GeneratedTask;
import com.javaacademy.platform.catalog.enums.Difficulty;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentArchitectServiceTest {

    LlmClient llmClient;
    ContentParser contentParser;
    CodeExecutionEngine executionEngine;
    ContentArchitectService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        contentParser = mock(ContentParser.class);
        executionEngine = mock(CodeExecutionEngine.class);
        service = new ContentArchitectService(llmClient, contentParser, executionEngine, "test-system-prompt");
    }

    // ── happy path ─────────────────────────────────────────────────────────────

    @Test
    void generateForTopic_firstAttemptPasses_returnsGeneratedContent() {
        GeneratedContent content = sampleContent();
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("{}", 10, 200));
        when(contentParser.parse(any())).thenReturn(content);
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.passed("Tests passed", 500L));

        GeneratedContent result = service.generateForTopic("Java Records");

        assertThat(result).isSameAs(content);
        verify(llmClient, times(1)).complete(any());
        verify(executionEngine, times(1)).execute(any());
    }

    // ── self-healing ───────────────────────────────────────────────────────────

    @Test
    void generateForTopic_firstAttemptFails_secondSucceeds() {
        GeneratedContent content = sampleContent();
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("{}", 10, 200));
        when(contentParser.parse(any())).thenReturn(content);
        when(executionEngine.execute(any()))
                .thenReturn(ExecutionResult.failed(2, "AssertionError at line 5", 300L))
                .thenReturn(ExecutionResult.passed("Tests passed", 400L));

        GeneratedContent result = service.generateForTopic("Java Records");

        assertThat(result).isSameAs(content);
        verify(llmClient, times(2)).complete(any());
        verify(executionEngine, times(2)).execute(any());
    }

    @Test
    void generateForTopic_selfHealingPrompt_includesPreviousError() {
        GeneratedContent content = sampleContent();
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("{}", 10, 200));
        when(contentParser.parse(any())).thenReturn(content);
        when(executionEngine.execute(any()))
                .thenReturn(ExecutionResult.failed(1, "expected 42 but was 0", 300L))
                .thenReturn(ExecutionResult.passed("OK", 200L));

        service.generateForTopic("Java Records");

        // Second LLM call must include the self-healing prefix and error output
        verify(llmClient, times(1))
                .complete(argThat(request -> request.userPrompt().contains("[SELF-HEALING ATTEMPT 2/3]")
                        && request.userPrompt().contains("expected 42 but was 0")));
    }

    // ── exhaustion ─────────────────────────────────────────────────────────────

    @Test
    void generateForTopic_allAttemptsExhausted_throwsLlmException() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("{}", 10, 200));
        when(contentParser.parse(any())).thenReturn(sampleContent());
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.failed(3, "still broken", 300L));

        assertThatThrownBy(() -> service.generateForTopic("Java Records"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("3 self-healing attempts");

        verify(llmClient, times(3)).complete(any());
        verify(executionEngine, times(3)).execute(any());
    }

    // ── parse failure ──────────────────────────────────────────────────────────

    @Test
    void generateForTopic_parseFailure_propagatesLlmException() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("bad json", 5, 10));
        when(contentParser.parse(any())).thenThrow(new LlmException("Invalid JSON"));

        assertThatThrownBy(() -> service.generateForTopic("Java Records")).isInstanceOf(LlmException.class);

        verify(executionEngine, times(0)).execute(any());
    }

    // ── sandbox timeout treated as failure ─────────────────────────────────────

    @Test
    void generateForTopic_sandboxTimeout_retriesAndPropagatesFailure() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmResponse("{}", 10, 200));
        when(contentParser.parse(any())).thenReturn(sampleContent());
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.timeout("Exceeded 5s limit"));

        assertThatThrownBy(() -> service.generateForTopic("Java Records")).isInstanceOf(LlmException.class);

        verify(llmClient, times(3)).complete(any());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static GeneratedContent sampleContent() {
        GeneratedLecture lecture = new GeneratedLecture("Java Records", "A".repeat(200));
        GeneratedTask task = new GeneratedTask("Create a Point", "B".repeat(60), Difficulty.EASY, 100);
        return new GeneratedContent(
                lecture,
                task,
                "public class Solution {}",
                "public class Solution { public int compute() { return 42; } }",
                "import org.junit.jupiter.api.Test;\npublic class TaskTest { @Test void t() {} }");
    }
}
