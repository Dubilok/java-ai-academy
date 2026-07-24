package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.entity.AiGenerationLog;
import com.javaacademy.platform.ai.enums.AgentType;
import com.javaacademy.platform.ai.enums.GenerationOutcome;
import com.javaacademy.platform.ai.repository.AiGenerationLogRepository;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import com.javaacademy.platform.sandbox.util.JavaClassNameExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContentArchitectService {

    static final int MAX_ATTEMPTS = 3;
    static final String DEFAULT_TEST_CLASS = "TaskTest";

    private final LlmClient llmClient;
    private final ContentParser contentParser;
    private final CodeExecutionEngine executionEngine;
    private final AiGenerationLogRepository generationLogRepository;
    private final Clock clock;
    private final String systemPrompt;

    @Autowired
    public ContentArchitectService(
            LlmClient llmClient,
            ContentParser contentParser,
            CodeExecutionEngine executionEngine,
            AiGenerationLogRepository generationLogRepository,
            Clock clock,
            @Value("classpath:prompts/content-architect-system.txt") Resource systemPromptResource)
            throws IOException {
        this.llmClient = llmClient;
        this.contentParser = contentParser;
        this.executionEngine = executionEngine;
        this.generationLogRepository = generationLogRepository;
        this.clock = clock;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Entry point for tests that inject the system prompt directly. */
    ContentArchitectService(
            LlmClient llmClient,
            ContentParser contentParser,
            CodeExecutionEngine executionEngine,
            AiGenerationLogRepository generationLogRepository,
            Clock clock,
            String systemPrompt) {
        this.llmClient = llmClient;
        this.contentParser = contentParser;
        this.executionEngine = executionEngine;
        this.generationLogRepository = generationLogRepository;
        this.clock = clock;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Generates a lecture + task for the given technology topic, running the self-healing loop
     * (max 3 attempts) until the generated tests pass the generated solution in the sandbox.
     *
     * <p>On success: saves a {@code SUCCEEDED} log entry and returns the content.
     * On exhaustion: saves an {@code EXHAUSTED} log entry and throws {@link LlmException} —
     * the caller never receives unverified content.
     *
     * @throws LlmException if all attempts are exhausted without a passing sandbox run,
     *     or if the LLM response cannot be parsed into valid {@link GeneratedContent}
     */
    public GeneratedContent generateForTopic(String technology) {
        String userPrompt = "Generate a lecture and programming task for: " + technology;
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String prompt = attempt == 1 ? userPrompt : buildSelfHealingPrompt(attempt, userPrompt, lastError);

            log.info("Content generation attempt {}/{} for '{}'", attempt, MAX_ATTEMPTS, technology);
            GeneratedContent content = callAndParse(prompt, attempt);
            ExecutionResult result = verifyInSandbox(content);

            if (result.status() == ExecutionStatus.PASSED) {
                log.info("Content generation succeeded on attempt {}/{} for '{}'", attempt, MAX_ATTEMPTS, technology);
                saveLog(GenerationOutcome.SUCCEEDED);
                return content;
            }

            lastError = result.logs();
            log.warn(
                    "Attempt {}/{} failed verification: {} failed tests. Logs: {}",
                    attempt,
                    MAX_ATTEMPTS,
                    result.failedTests(),
                    truncate(lastError, 500));
        }

        saveLog(GenerationOutcome.EXHAUSTED);
        throw new LlmException(
                "Content generation for '" + technology + "' failed after " + MAX_ATTEMPTS + " self-healing attempts");
    }

    private GeneratedContent callAndParse(String userPrompt, int attempt) {
        LlmRequest request = new LlmRequest(null, systemPrompt, userPrompt, 4096);
        LlmResponse response = llmClient.complete(request);

        try {
            return contentParser.parse(response.content());
        } catch (LlmException parseException) {
            saveLog(GenerationOutcome.PARSE_FAILED);
            throw new LlmException("Attempt " + attempt + " parse failure: " + parseException.getMessage());
        }
    }

    private ExecutionResult verifyInSandbox(GeneratedContent content) {
        String testClassName = JavaClassNameExtractor.extractPublicClassName(content.testCode())
                .orElse(DEFAULT_TEST_CLASS);

        ExecutionRequest request =
                new ExecutionRequest(UUID.randomUUID(), content.solutionCode(), content.testCode(), testClassName);
        return executionEngine.execute(request);
    }

    private void saveLog(GenerationOutcome outcome) {
        AiGenerationLog logEntry = new AiGenerationLog();
        logEntry.setAgent(AgentType.CONTENT_ARCHITECT);
        logEntry.setModel("content-architect");
        logEntry.setOutcome(outcome);
        logEntry.setCreatedAt(Instant.now(clock));
        generationLogRepository.save(logEntry);
    }

    private static String buildSelfHealingPrompt(int attempt, String originalPrompt, String errorOutput) {
        return "[SELF-HEALING ATTEMPT " + attempt + "/" + MAX_ATTEMPTS + "]\n\n"
                + "Original topic: " + originalPrompt + "\n\n"
                + "The previous generation attempt failed automated sandbox verification.\n"
                + "Fix the failing code. Do not change the lecture or task narrative.\n\n"
                + "<ERROR_OUTPUT>\n"
                + errorOutput
                + "\n</ERROR_OUTPUT>";
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
