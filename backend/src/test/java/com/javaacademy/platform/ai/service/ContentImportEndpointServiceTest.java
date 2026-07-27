package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.dto.GeneratedLecture;
import com.javaacademy.platform.ai.dto.GeneratedTask;
import com.javaacademy.platform.ai.dto.ImportContentResponse;
import com.javaacademy.platform.catalog.enums.Difficulty;
import com.javaacademy.platform.catalog.service.ContentImportService;
import com.javaacademy.platform.catalog.service.ContentImportService.ImportResult;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ContentImportEndpointServiceTest {

    ContentParser contentParser;
    CodeExecutionEngine executionEngine;
    ContentImportService contentImportService;
    ContentImportEndpointService service;

    @BeforeEach
    void setUp() {
        contentParser = mock(ContentParser.class);
        executionEngine = mock(CodeExecutionEngine.class);
        contentImportService = mock(ContentImportService.class);
        service = new ContentImportEndpointService(contentParser, executionEngine, contentImportService);
    }

    // ── happy path ─────────────────────────────────────────────────────────────

    @Test
    void importFromJson_validJsonAndSandboxPasses_returnsResponse() {
        UUID moduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID lectureId = UUID.randomUUID();
        GeneratedContent content = sampleContent();

        when(contentParser.parse("{\"valid\":true}")).thenReturn(content);
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.passed("All tests pass", 500L));
        when(contentImportService.importLectureAndTask(eq(moduleId), eq(content)))
                .thenReturn(new ImportResult(courseId, lectureId));

        ImportContentResponse response = service.importFromJson(moduleId, "{\"valid\":true}");

        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.lectureId()).isEqualTo(lectureId);
    }

    // ── validation / parse failure ─────────────────────────────────────────────

    @Test
    void importFromJson_invalidJson_throws422WithParseMessage() {
        UUID moduleId = UUID.randomUUID();
        when(contentParser.parse(any())).thenThrow(new LlmException("Missing required field: lecture.title"));

        assertThatThrownBy(() -> service.importFromJson(moduleId, "bad json"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Missing required field: lecture.title")
                .satisfies(
                        ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(executionEngine, never()).execute(any());
    }

    // ── sandbox failure ────────────────────────────────────────────────────────

    @Test
    void importFromJson_sandboxFails_throws422WithLogs() {
        UUID moduleId = UUID.randomUUID();
        when(contentParser.parse(any())).thenReturn(sampleContent());
        when(executionEngine.execute(any()))
                .thenReturn(ExecutionResult.failed(2, "AssertionError: expected 42 but was 0", 300L));

        assertThatThrownBy(() -> service.importFromJson(moduleId, "{}"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("sandbox verification")
                .hasMessageContaining("AssertionError");

        verify(contentImportService, never()).importLectureAndTask(any(), any());
    }

    @Test
    void importFromJson_sandboxTimeout_throws422() {
        UUID moduleId = UUID.randomUUID();
        when(contentParser.parse(any())).thenReturn(sampleContent());
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.timeout("Exceeded 5s wall clock limit"));

        assertThatThrownBy(() -> service.importFromJson(moduleId, "{}"))
                .isInstanceOf(ApiException.class)
                .satisfies(
                        ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(contentImportService, never()).importLectureAndTask(any(), any());
    }

    @Test
    void importFromJson_sandboxFailsWithNullLogs_throws422WithPlaceholder() {
        UUID moduleId = UUID.randomUUID();
        when(contentParser.parse(any())).thenReturn(sampleContent());
        when(executionEngine.execute(any())).thenReturn(new ExecutionResult(ExecutionStatus.FAILED, 1, null, 200L));

        assertThatThrownBy(() -> service.importFromJson(moduleId, "{}"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("(no output)");
    }

    // ── log truncation ─────────────────────────────────────────────────────────

    @Test
    void importFromJson_sandboxFailsWithLargeLog_truncatesAt1000Chars() {
        UUID moduleId = UUID.randomUUID();
        String longLog = "x".repeat(2000);
        when(contentParser.parse(any())).thenReturn(sampleContent());
        when(executionEngine.execute(any())).thenReturn(ExecutionResult.failed(1, longLog, 300L));

        assertThatThrownBy(() -> service.importFromJson(moduleId, "{}"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage();
                    assertThat(message).endsWith("…");
                    // message is "Solution code failed sandbox verification (FAILED): " + truncated log + "…"
                    assertThat(message.length()).isLessThan(2000);
                });
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
