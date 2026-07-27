package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.AnthropicProperties;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.GeneratedContent;
import com.javaacademy.platform.ai.entity.AiGenerationLog;
import com.javaacademy.platform.ai.enums.AgentType;
import com.javaacademy.platform.ai.enums.GenerationOutcome;
import com.javaacademy.platform.ai.repository.AiGenerationLogRepository;
import com.javaacademy.platform.config.PlatformMetrics;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import com.javaacademy.platform.sandbox.util.JavaClassNameExtractor;
import java.io.IOException;
import java.math.BigDecimal;
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
    private final AnthropicProperties anthropicProperties;
    private final PlatformMetrics metrics;
    private final Clock clock;
    private final String systemPrompt;

    @Autowired
    public ContentArchitectService(
            LlmClient llmClient,
            ContentParser contentParser,
            CodeExecutionEngine executionEngine,
            AiGenerationLogRepository generationLogRepository,
            AnthropicProperties anthropicProperties,
            PlatformMetrics metrics,
            Clock clock,
            @Value("classpath:prompts/content-architect-system.txt") Resource systemPromptResource)
            throws IOException {
        this.llmClient = llmClient;
        this.contentParser = contentParser;
        this.executionEngine = executionEngine;
        this.generationLogRepository = generationLogRepository;
        this.anthropicProperties = anthropicProperties;
        this.metrics = metrics;
        this.clock = clock;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Entry point for tests that inject the system prompt directly. */
    ContentArchitectService(
            LlmClient llmClient,
            ContentParser contentParser,
            CodeExecutionEngine executionEngine,
            AiGenerationLogRepository generationLogRepository,
            AnthropicProperties anthropicProperties,
            PlatformMetrics metrics,
            Clock clock,
            String systemPrompt) {
        this.llmClient = llmClient;
        this.contentParser = contentParser;
        this.executionEngine = executionEngine;
        this.generationLogRepository = generationLogRepository;
        this.anthropicProperties = anthropicProperties;
        this.metrics = metrics;
        this.clock = clock;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Generates a lecture + task for the given technology topic, running the self-healing loop
     * (max 3 attempts) until the generated tests pass the generated solution in the sandbox.
     *
     * <p>On success: saves a {@code SUCCEEDED} log entry with token/cost/latency telemetry and returns the content.
     * On exhaustion: saves an {@code EXHAUSTED} log entry and throws {@link LlmException} —
     * the caller never receives unverified content.
     *
     * @throws LlmException if all attempts are exhausted without a passing sandbox run,
     *     or if the LLM response cannot be parsed into valid {@link GeneratedContent}
     */
    public GeneratedContent generateForTopic(String technology) {
        long startMs = clock.millis();
        String userPrompt = "Generate a lecture and programming task for: " + technology;
        String lastError = null;
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String prompt = attempt == 1 ? userPrompt : buildSelfHealingPrompt(attempt, userPrompt, lastError);
            String model = attempt == 1
                    ? anthropicProperties.effectiveGenerationModel()
                    : anthropicProperties.effectiveHealingModel();

            log.info(
                    "Content generation attempt {}/{} for '{}' using model '{}'",
                    attempt,
                    MAX_ATTEMPTS,
                    technology,
                    model);
            ParseResult parseResult =
                    callAndParse(prompt, model, attempt, startMs, totalPromptTokens, totalCompletionTokens);
            totalPromptTokens = parseResult.totalPromptTokens;
            totalCompletionTokens = parseResult.totalCompletionTokens;

            ExecutionResult result = verifyInSandbox(parseResult.content);

            if (result.status() == ExecutionStatus.PASSED) {
                long latencyMs = clock.millis() - startMs;
                log.info("Content generation succeeded on attempt {}/{} for '{}'", attempt, MAX_ATTEMPTS, technology);
                saveLog(GenerationOutcome.SUCCEEDED, totalPromptTokens, totalCompletionTokens, latencyMs);
                return parseResult.content;
            }

            lastError = result.logs();
            log.warn(
                    "Attempt {}/{} failed verification: {} failed tests. Logs: {}",
                    attempt,
                    MAX_ATTEMPTS,
                    result.failedTests(),
                    truncate(lastError, 500));
        }

        long latencyMs = clock.millis() - startMs;
        saveLog(GenerationOutcome.EXHAUSTED, totalPromptTokens, totalCompletionTokens, latencyMs);
        throw new LlmException(
                "Content generation for '" + technology + "' failed after " + MAX_ATTEMPTS + " self-healing attempts");
    }

    private ParseResult callAndParse(
            String userPrompt,
            String model,
            int attempt,
            long startMs,
            int accumulatedPromptTokens,
            int accumulatedCompletionTokens) {
        LlmRequest request = new LlmRequest(model, systemPrompt, userPrompt, 8192);
        LlmResponse response = llmClient.complete(request);
        int newPromptTokens = accumulatedPromptTokens + response.promptTokens();
        int newCompletionTokens = accumulatedCompletionTokens + response.completionTokens();

        try {
            GeneratedContent content = contentParser.parse(response.content());
            return new ParseResult(content, newPromptTokens, newCompletionTokens);
        } catch (LlmException parseException) {
            long latencyMs = clock.millis() - startMs;
            saveLog(GenerationOutcome.PARSE_FAILED, newPromptTokens, newCompletionTokens, latencyMs);
            throw new LlmException("Attempt " + attempt + " parse failure: " + parseException.getMessage());
        }
    }

    private ExecutionResult verifyInSandbox(GeneratedContent content) {
        String testClassName = JavaClassNameExtractor.extractPublicClassName(content.testCode())
                .orElse(DEFAULT_TEST_CLASS);

        ExecutionRequest executionRequest =
                new ExecutionRequest(UUID.randomUUID(), content.solutionCode(), content.testCode(), testClassName);
        return executionEngine.execute(executionRequest);
    }

    private void saveLog(GenerationOutcome outcome, int promptTokens, int completionTokens, long latencyMs) {
        BigDecimal costUsd = computeCost(promptTokens, completionTokens);
        AiGenerationLog logEntry = new AiGenerationLog();
        logEntry.setAgent(AgentType.CONTENT_ARCHITECT);
        logEntry.setModel(anthropicProperties.defaultModel());
        logEntry.setOutcome(outcome);
        logEntry.setPromptTokens(promptTokens);
        logEntry.setCompletionTokens(completionTokens);
        logEntry.setCostUsd(costUsd);
        logEntry.setLatencyMs(latencyMs);
        logEntry.setCreatedAt(Instant.now(clock));
        generationLogRepository.save(logEntry);
        metrics.recordTokens(promptTokens, completionTokens);
        log.info(
                "Generation log saved: outcome={} promptTokens={} completionTokens={} costUsd={} latencyMs={}",
                outcome,
                promptTokens,
                completionTokens,
                costUsd,
                latencyMs);
    }

    private BigDecimal computeCost(int promptTokens, int completionTokens) {
        double inputCost = promptTokens * anthropicProperties.inputCostPerMillionTokens() / 1_000_000.0;
        double outputCost = completionTokens * anthropicProperties.outputCostPerMillionTokens() / 1_000_000.0;
        return BigDecimal.valueOf(inputCost + outputCost).setScale(6, java.math.RoundingMode.HALF_UP);
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

    private record ParseResult(GeneratedContent content, int totalPromptTokens, int totalCompletionTokens) {}
}
