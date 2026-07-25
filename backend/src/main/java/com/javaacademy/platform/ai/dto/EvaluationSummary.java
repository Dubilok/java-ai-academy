package com.javaacademy.platform.ai.dto;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** DTO for the quality dashboard — one row per ai_evaluation record. */
public record EvaluationSummary(
        UUID id,
        String targetType,
        @Nullable UUID targetId,
        @Nullable JudgeScores scores,
        @Nullable String flags,
        Instant createdAt) {}
