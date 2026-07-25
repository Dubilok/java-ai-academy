package com.javaacademy.platform.interview.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record EvaluationReport(
        @NotNull UUID sessionId,
        @NotBlank String technology,
        @NotNull @Min(0) @Max(100) Integer overallScore,
        @NotNull @Size(min = 10, max = 10) @Valid List<EvaluationDimension> dimensions,
        @NotNull Instant completedAt,
        @Nullable List<RagCitation> citations) {}
