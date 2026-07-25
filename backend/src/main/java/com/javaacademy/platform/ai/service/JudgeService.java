package com.javaacademy.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.dto.JudgeScores;
import com.javaacademy.platform.ai.entity.AiEvaluation;
import com.javaacademy.platform.ai.enums.EvaluationTargetType;
import com.javaacademy.platform.ai.repository.AiEvaluationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * A5 Judge: asynchronously scores AI-generated content for hallucination,
 * answer-leakage, and rubric adherence. Writes results to {@code ai_evaluation}.
 * Never sits in the request path — all evaluation methods are {@code @Async}.
 */
@Slf4j
@Service
public class JudgeService {

    static final int MAX_JUDGE_TOKENS = 400;
    static final int HALLUCINATION_FLAG_THRESHOLD = 4;
    static final int RUBRIC_FLAG_THRESHOLD = 4;

    private final LlmClient llmClient;
    private final AiEvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String systemPrompt;

    @Autowired
    public JudgeService(
            @Qualifier("anthropicLlmClient") LlmClient llmClient,
            AiEvaluationRepository evaluationRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("classpath:prompts/judge-system.txt") Resource systemPromptResource) {
        this.llmClient = llmClient;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.systemPrompt = loadResource(systemPromptResource);
    }

    /** For testing: inject system prompt directly. */
    JudgeService(
            LlmClient llmClient,
            AiEvaluationRepository evaluationRepository,
            ObjectMapper objectMapper,
            Clock clock,
            String systemPrompt) {
        this.llmClient = llmClient;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.systemPrompt = systemPrompt;
    }

    @Async
    public void evaluateHintAsync(UUID hintId, String taskTitle, @Nullable String taskDescription, String hintText) {
        try {
            String context =
                    "Task title: " + taskTitle + (taskDescription != null ? "\nDescription: " + taskDescription : "");
            doEvaluate(EvaluationTargetType.HINT, hintId, context, hintText);
        } catch (Exception exception) {
            log.error("Judge evaluation failed for hint {}: {}", hintId, exception.getMessage());
        }
    }

    @Async
    public void evaluateInterviewEvaluationAsync(UUID sessionId, String technology, String evaluationJson) {
        try {
            String context = "Technology: " + technology;
            doEvaluate(EvaluationTargetType.INTERVIEW_EVALUATION, sessionId, context, evaluationJson);
        } catch (Exception exception) {
            log.error("Judge evaluation failed for interview session {}: {}", sessionId, exception.getMessage());
        }
    }

    void doEvaluate(EvaluationTargetType targetType, UUID targetId, String context, String output) {
        String userPrompt = buildUserPrompt(targetType, context, output);
        LlmRequest request = new LlmRequest(null, systemPrompt, userPrompt, MAX_JUDGE_TOKENS);

        String rawJson = llmClient.complete(request).content();
        JudgeScores scores = parseScores(rawJson);

        String flags = buildFlags(scores);

        AiEvaluation evaluation = new AiEvaluation();
        evaluation.setTargetType(targetType.name());
        evaluation.setTargetId(targetId);
        evaluation.setJudgeScoresJson(rawJson);
        evaluation.setFlags(flags.isEmpty() ? null : flags);
        evaluation.setCreatedAt(Instant.now(clock));
        evaluationRepository.save(evaluation);

        log.info(
                "Judge evaluated {} target={}: hallucination={} leakage={} rubric={}",
                targetType,
                targetId,
                scores.hallucinationScore(),
                scores.answerLeakageDetected(),
                scores.rubricAdherenceScore());
    }

    JudgeScores parseScores(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, JudgeScores.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Judge returned invalid JSON: " + exception.getOriginalMessage());
        }
    }

    private static String buildUserPrompt(EvaluationTargetType targetType, String context, String output) {
        return "Target type: " + targetType.name() + "\n\n"
                + "<CONTEXT>\n" + context + "\n</CONTEXT>\n\n"
                + "<TARGET_OUTPUT>\n" + output + "\n</TARGET_OUTPUT>";
    }

    private static String buildFlags(JudgeScores scores) {
        List<String> flags = new ArrayList<>();
        if (scores.answerLeakageDetected()) flags.add("ANSWER_LEAKED");
        if (scores.hallucinationScore() <= HALLUCINATION_FLAG_THRESHOLD) flags.add("LOW_HALLUCINATION_SCORE");
        if (scores.rubricAdherenceScore() <= RUBRIC_FLAG_THRESHOLD) flags.add("LOW_RUBRIC_ADHERENCE");
        return String.join(",", flags);
    }

    private static String loadResource(Resource resource) {
        try {
            return resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
                    .trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load judge system prompt", exception);
        }
    }
}
