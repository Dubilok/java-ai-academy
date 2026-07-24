package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.HintRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SocraticMentorServiceTest {

    LlmClient llmClient;
    SocraticMentorService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        service = new SocraticMentorService(llmClient, "You are a Socratic mentor — no solution code.");
    }

    // ── happy path ─────────────────────────────────────────────────────────────

    @Test
    void generateHint_validRequest_returnsLlmContent() {
        when(llmClient.complete(argThat(request -> request != null)))
                .thenReturn(
                        new LlmResponse("Have you considered what the compiler means by 'symbol not found'?", 50, 30));

        String hint = service.generateHint(hintRequest("return 0;", "AssertionError: expected 5 but was 0"));

        assertThat(hint).isEqualTo("Have you considered what the compiler means by 'symbol not found'?");
    }

    // ── prompt grounding ───────────────────────────────────────────────────────

    @Test
    void generateHint_prompt_containsTaskTitle() {
        when(llmClient.complete(argThat(request -> request != null)))
                .thenReturn(new LlmResponse("Good question!", 20, 15));

        service.generateHint(hintRequest("return 0;", "AssertionError"));

        verify(llmClient)
                .complete(argThat((LlmRequest request) -> request.userPrompt().contains("Implement a Point record")));
    }

    @Test
    void generateHint_prompt_containsTaskDescription() {
        when(llmClient.complete(argThat(request -> request != null)))
                .thenReturn(new LlmResponse("Think about this.", 20, 15));

        service.generateHint(hintRequest("return 0;", "AssertionError"));

        verify(llmClient)
                .complete(argThat((LlmRequest request) -> request.userPrompt().contains("Euclidean distance")));
    }

    @Test
    void generateHint_prompt_containsStudentSource() {
        when(llmClient.complete(argThat(request -> request != null))).thenReturn(new LlmResponse("Hint text", 20, 15));

        service.generateHint(hintRequest("public double distance() { return 0; }", "AssertionError"));

        verify(llmClient)
                .complete(argThat((LlmRequest request) -> request.userPrompt().contains("public double distance()")));
    }

    @Test
    void generateHint_prompt_containsErrorOutput() {
        when(llmClient.complete(argThat(request -> request != null))).thenReturn(new LlmResponse("Hint text", 20, 15));

        service.generateHint(hintRequest("return 0;", "AssertionError: expected 5.0 but was 0.0 at line 12"));

        verify(llmClient).complete(argThat((LlmRequest request) -> request.userPrompt()
                .contains("expected 5.0 but was 0.0 at line 12")));
    }

    @Test
    void generateHint_prompt_wrapsContentInDelimitedBlocks() {
        when(llmClient.complete(argThat(request -> request != null))).thenReturn(new LlmResponse("Hint", 10, 8));

        service.generateHint(hintRequest("return 0;", "error"));

        verify(llmClient).complete(argThat((LlmRequest request) -> {
            String prompt = request.userPrompt();
            return prompt.contains("<TASK_DESCRIPTION>")
                    && prompt.contains("</TASK_DESCRIPTION>")
                    && prompt.contains("<STUDENT_CODE>")
                    && prompt.contains("</STUDENT_CODE>")
                    && prompt.contains("<ERROR_OUTPUT>")
                    && prompt.contains("</ERROR_OUTPUT>");
        }));
    }

    // ── edge cases ─────────────────────────────────────────────────────────────

    @Test
    void generateHint_nullErrorOutput_usesPlaceholder() {
        when(llmClient.complete(argThat(request -> request != null))).thenReturn(new LlmResponse("Hint", 10, 8));

        service.generateHint(new HintRequest(
                "Implement a Point record", "Create a record that computes Euclidean distance.", "return 0;", null));

        verify(llmClient)
                .complete(argThat((LlmRequest request) -> request.userPrompt().contains("no error output available")));
    }

    @Test
    void generateHint_longStudentSource_isTruncated() {
        String oversizedSource = "x".repeat(SocraticMentorService.MAX_CODE_CHARS + 500);
        when(llmClient.complete(argThat(request -> request != null))).thenReturn(new LlmResponse("Hint", 10, 8));

        service.generateHint(new HintRequest(
                "Implement a Point record",
                "Create a record that computes Euclidean distance.",
                oversizedSource,
                "error"));

        verify(llmClient).complete(argThat((LlmRequest request) -> {
            String prompt = request.userPrompt();
            return prompt.contains("[truncated]") && prompt.length() < oversizedSource.length() + 1000;
        }));
    }

    @Test
    void generateHint_longErrorOutput_isTruncated() {
        String oversizedError = "e".repeat(SocraticMentorService.MAX_ERROR_CHARS + 500);
        when(llmClient.complete(argThat(request -> request != null))).thenReturn(new LlmResponse("Hint", 10, 8));

        service.generateHint(new HintRequest(
                "Implement a Point record",
                "Create a record that computes Euclidean distance.",
                "return 0;",
                oversizedError));

        verify(llmClient).complete(argThat((LlmRequest request) -> {
            String prompt = request.userPrompt();
            return prompt.contains("[truncated]");
        }));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static HintRequest hintRequest(String studentSource, String errorOutput) {
        return new HintRequest(
                "Implement a Point record",
                "Create a record that computes the Euclidean distance between two points.",
                studentSource,
                errorOutput);
    }
}
