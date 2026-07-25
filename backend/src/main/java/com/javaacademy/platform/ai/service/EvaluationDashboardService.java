package com.javaacademy.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.dto.EvaluationSummary;
import com.javaacademy.platform.ai.dto.JudgeScores;
import com.javaacademy.platform.ai.entity.AiEvaluation;
import com.javaacademy.platform.ai.repository.AiEvaluationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationDashboardService {

    private final AiEvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<EvaluationSummary> getRecentEvaluations(@Nullable String targetType) {
        List<AiEvaluation> evaluations = targetType != null
                ? evaluationRepository.findByTargetTypeOrderByCreatedAtDesc(targetType)
                : evaluationRepository.findTop50ByOrderByCreatedAtDesc();
        return evaluations.stream().map(this::toSummary).toList();
    }

    private EvaluationSummary toSummary(AiEvaluation evaluation) {
        JudgeScores scores = null;
        String json = evaluation.getJudgeScoresJson();
        if (json != null) {
            try {
                scores = objectMapper.readValue(json, JudgeScores.class);
            } catch (JsonProcessingException exception) {
                log.warn("Unparseable judge scores for evaluation {}: {}", evaluation.getId(), exception.getMessage());
            }
        }
        return new EvaluationSummary(
                evaluation.getId(),
                evaluation.getTargetType(),
                evaluation.getTargetId(),
                scores,
                evaluation.getFlags(),
                evaluation.getCreatedAt());
    }
}
