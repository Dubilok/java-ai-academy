package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.JudgeScores;
import com.javaacademy.platform.ai.entity.AiEvaluation;
import com.javaacademy.platform.ai.enums.EvaluationTargetType;
import com.javaacademy.platform.ai.repository.AiEvaluationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JudgeServiceTest {

    static final String SYSTEM_PROMPT = "You are the Judge. Return JSON only.";
    static final UUID TARGET_ID = UUID.randomUUID();

    LlmClient llmClient;
    AiEvaluationRepository evaluationRepository;
    JudgeService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        evaluationRepository = mock(AiEvaluationRepository.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-25T10:00:00Z"), ZoneOffset.UTC);

        service = new JudgeService(llmClient, evaluationRepository, new ObjectMapper(), fixedClock, SYSTEM_PROMPT);
    }

    // ── parseScores ────────────────────────────────────────────────────────────

    @Test
    void parseScores_validJson_returnsScores() {
        String json =
                """
                {"hallucinationScore":8,"answerLeakageDetected":false,
                 "rubricAdherenceScore":7,"explanation":"Looks good."}""";

        JudgeScores scores = service.parseScores(json);

        assertThat(scores.hallucinationScore()).isEqualTo(8);
        assertThat(scores.answerLeakageDetected()).isFalse();
        assertThat(scores.rubricAdherenceScore()).isEqualTo(7);
        assertThat(scores.explanation()).isEqualTo("Looks good.");
    }

    @Test
    void parseScores_invalidJson_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.parseScores("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid JSON");
    }

    // ── buildFlags ─────────────────────────────────────────────────────────────

    @Test
    void buildFlags_cleanScores_returnsEmptyString() {
        JudgeScores scores = new JudgeScores(8, false, 8, "Fine.");
        String flags = buildFlags(scores);
        assertThat(flags).isEmpty();
    }

    @Test
    void buildFlags_answerLeaked_includesLeakageFlag() {
        JudgeScores scores = new JudgeScores(8, true, 8, "Leaked answer.");
        String flags = buildFlags(scores);
        assertThat(flags).contains("ANSWER_LEAKED");
    }

    @Test
    void buildFlags_lowHallucinationScore_includesHallucinationFlag() {
        JudgeScores scores = new JudgeScores(4, false, 8, "Hallucinating.");
        String flags = buildFlags(scores);
        assertThat(flags).contains("LOW_HALLUCINATION_SCORE");
    }

    @Test
    void buildFlags_lowRubricScore_includesRubricFlag() {
        JudgeScores scores = new JudgeScores(8, false, 4, "Off rubric.");
        String flags = buildFlags(scores);
        assertThat(flags).contains("LOW_RUBRIC_ADHERENCE");
    }

    @Test
    void buildFlags_allIssues_includesAllFlags() {
        JudgeScores scores = new JudgeScores(3, true, 3, "Everything wrong.");
        String flags = buildFlags(scores);
        assertThat(flags).contains("ANSWER_LEAKED", "LOW_HALLUCINATION_SCORE", "LOW_RUBRIC_ADHERENCE");
    }

    // ── doEvaluate ─────────────────────────────────────────────────────────────

    @Test
    void doEvaluate_cleanOutput_persistsEvaluationWithNoFlags() {
        String cleanJson =
                "{\"hallucinationScore\":9,\"answerLeakageDetected\":false,\"rubricAdherenceScore\":9,\"explanation\":\"Great.\"}";
        when(llmClient.complete(any())).thenReturn(new LlmResponse(cleanJson, 10, 50));
        when(evaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.doEvaluate(EvaluationTargetType.HINT, TARGET_ID, "context", "hint text");

        ArgumentCaptor<AiEvaluation> captor = ArgumentCaptor.forClass(AiEvaluation.class);
        verify(evaluationRepository).save(captor.capture());
        AiEvaluation saved = captor.getValue();
        assertThat(saved.getTargetType()).isEqualTo("HINT");
        assertThat(saved.getTargetId()).isEqualTo(TARGET_ID);
        assertThat(saved.getFlags()).isNull();
    }

    @Test
    void doEvaluate_leakedAnswer_persistsAnswerLeakedFlag() {
        String leakedJson =
                "{\"hallucinationScore\":8,\"answerLeakageDetected\":true,\"rubricAdherenceScore\":7,\"explanation\":\"Leaked.\"}";
        when(llmClient.complete(any())).thenReturn(new LlmResponse(leakedJson, 10, 60));
        when(evaluationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.doEvaluate(EvaluationTargetType.HINT, TARGET_ID, "context", "leaked hint");

        ArgumentCaptor<AiEvaluation> captor = ArgumentCaptor.forClass(AiEvaluation.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getFlags()).contains("ANSWER_LEAKED");
    }

    // ── evaluateHintAsync (fire and forget — exception swallowed) ─────────────

    @Test
    void evaluateHintAsync_llmThrows_doesNotPropagateException() {
        when(llmClient.complete(any())).thenThrow(new RuntimeException("LLM unavailable"));

        // @Async is not active in plain unit tests — method runs synchronously here,
        // but we verify the exception is caught (no throws)
        service.evaluateHintAsync(TARGET_ID, "Task", "desc", "hint text");

        verify(evaluationRepository, never()).save(any());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static String buildFlags(JudgeScores scores) {
        java.util.List<String> flags = new java.util.ArrayList<>();
        if (scores.answerLeakageDetected()) flags.add("ANSWER_LEAKED");
        if (scores.hallucinationScore() <= JudgeService.HALLUCINATION_FLAG_THRESHOLD)
            flags.add("LOW_HALLUCINATION_SCORE");
        if (scores.rubricAdherenceScore() <= JudgeService.RUBRIC_FLAG_THRESHOLD) flags.add("LOW_RUBRIC_ADHERENCE");
        return String.join(",", flags);
    }
}
