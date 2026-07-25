package com.javaacademy.platform.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record AiUsageResponse(
        @Nullable Instant from,
        @Nullable Instant to,
        long totalRequests,
        long totalPromptTokens,
        long totalCompletionTokens,
        @Nullable BigDecimal totalCostUsd,
        List<AgentUsageSummary> byAgent,
        List<UserUsageSummary> byUser,
        List<CourseUsageSummary> byCourse) {}
