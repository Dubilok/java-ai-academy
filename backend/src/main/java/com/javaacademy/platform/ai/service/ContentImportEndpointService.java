package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.dto.ImportContentResponse;
import com.javaacademy.platform.catalog.service.ContentImportService;
import com.javaacademy.platform.catalog.service.ContentImportService.ImportResult;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import com.javaacademy.platform.sandbox.util.JavaClassNameExtractor;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the chat-to-import flow: parse raw JSON → sandbox verify → persist.
 * No LLM call is made — the content was authored externally (e.g. Claude.ai chat).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentImportEndpointService {

    private static final String DEFAULT_TEST_CLASS = "TaskTest";

    private final ContentParser contentParser;
    private final CodeExecutionEngine executionEngine;
    private final ContentImportService contentImportService;

    /**
     * Parses {@code rawJson}, verifies the code in the sandbox, and persists to the given module.
     *
     * @throws ApiException 422 if JSON is invalid or fails validation
     * @throws ApiException 422 if the sandbox rejects the solution (includes failure logs)
     */
    public ImportContentResponse importFromJson(UUID moduleId, String rawJson) {
        GeneratedContent content = parseAndValidate(rawJson);
        verifyInSandbox(content);
        ImportResult result = contentImportService.importLectureAndTask(moduleId, content);
        log.info("Imported lecture {} into module {}", result.lectureId(), moduleId);
        return new ImportContentResponse(result.courseId(), result.lectureId());
    }

    private GeneratedContent parseAndValidate(String rawJson) {
        try {
            return contentParser.parse(rawJson);
        } catch (LlmException parseException) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, parseException.getMessage());
        }
    }

    private void verifyInSandbox(GeneratedContent content) {
        String testClassName = JavaClassNameExtractor.extractPublicClassName(content.testCode())
                .orElse(DEFAULT_TEST_CLASS);
        ExecutionRequest request =
                new ExecutionRequest(UUID.randomUUID(), content.solutionCode(), content.testCode(), testClassName);
        ExecutionResult result = executionEngine.execute(request);

        if (result.status() != ExecutionStatus.PASSED) {
            String logs = result.logs() != null ? result.logs() : "(no output)";
            String truncated = logs.length() > 1000 ? logs.substring(0, 1000) + "…" : logs;
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Solution code failed sandbox verification (" + result.status() + "): " + truncated);
        }
    }
}
