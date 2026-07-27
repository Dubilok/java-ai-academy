package com.javaacademy.platform.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.interview.dto.GenerateFlashcardsRequest;
import com.javaacademy.platform.interview.dto.InterviewQuestionResponse;
import com.javaacademy.platform.interview.entity.InterviewQuestion;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.mapper.InterviewQuestionMapper;
import com.javaacademy.platform.interview.repository.InterviewQuestionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FlashcardGeneratorService {

    private final LlmClient llmClient;
    private final InterviewQuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;

    public FlashcardGeneratorService(
            @Qualifier("anthropicLlmClient") LlmClient llmClient,
            InterviewQuestionRepository questionRepository,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.systemPrompt = loadSystemPrompt();
    }

    @Transactional
    public List<InterviewQuestionResponse> generate(GenerateFlashcardsRequest request) {
        String userPrompt = buildUserPrompt(request);

        log.info(
                "Generating {} flashcards for technology={} category={} difficulty={}",
                request.count(),
                request.technology(),
                request.category(),
                request.difficulty());

        String raw = llmClient
                .complete(new LlmRequest(null, systemPrompt, userPrompt, 4096))
                .content();

        List<Map<String, Object>> parsed = parseResponse(raw);

        List<InterviewQuestion> saved = parsed.stream()
                .limit(request.count())
                .map(item -> toEntity(item, request.technology()))
                .map(questionRepository::save)
                .toList();

        log.info("Saved {} flashcards for technology={}", saved.size(), request.technology());
        return saved.stream().map(InterviewQuestionMapper::toResponse).toList();
    }

    private String buildUserPrompt(GenerateFlashcardsRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate exactly ").append(request.count());
        prompt.append(" interview flashcards for: ").append(request.technology());
        if (request.category() != null && !request.category().isBlank()) {
            prompt.append(", category: ").append(request.category());
        }
        if (request.difficulty() != null) {
            prompt.append(", difficulty: ").append(request.difficulty());
        } else {
            prompt.append(", difficulty: mix of BEGINNER, INTERMEDIATE, and ADVANCED");
        }
        prompt.append(".\n\nReturn a JSON array of ").append(request.count()).append(" objects.");
        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            trimmed = trimmed.substring(start, end > start ? end : trimmed.length()).strip();
        }
        try {
            return objectMapper.readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception parseException) {
            log.error("Failed to parse flashcard JSON from LLM response: {}", trimmed, parseException);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "AI returned an invalid response — please try again.");
        }
    }

    private InterviewQuestion toEntity(Map<String, Object> item, String defaultTechnology) {
        InterviewQuestion question = new InterviewQuestion();
        question.setTechnology(stringOrDefault(item.get("technology"), defaultTechnology));
        question.setCategory(stringOrDefault(item.get("category"), "General"));
        question.setQuestion(stringOrDefault(item.get("question"), ""));
        question.setShortAnswer(stringOrDefault(item.get("shortAnswer"), null));
        question.setDetailedExplanation(stringOrDefault(item.get("detailedExplanation"), null));
        question.setDifficulty(parseDifficulty(item.get("difficulty")));
        return question;
    }

    private static @Nullable String stringOrDefault(@Nullable Object value, @Nullable String fallback) {
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return fallback;
    }

    private static InterviewDifficulty parseDifficulty(@Nullable Object value) {
        if (value instanceof String str) {
            try {
                return InterviewDifficulty.valueOf(str.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to default
            }
        }
        return InterviewDifficulty.INTERMEDIATE;
    }

    private static String loadSystemPrompt() {
        try (InputStream stream = FlashcardGeneratorService.class
                .getResourceAsStream("/prompts/flashcard-generator-system.txt")) {
            if (stream == null) {
                throw new IllegalStateException("flashcard-generator-system.txt not found on classpath");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to load flashcard generator system prompt", ioException);
        }
    }
}
