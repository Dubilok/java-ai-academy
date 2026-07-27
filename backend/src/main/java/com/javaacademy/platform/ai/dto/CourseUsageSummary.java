package com.javaacademy.platform.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CourseUsageSummary(
        UUID courseId,
        String title,
        long requestCount,
        long promptTokens,
        long completionTokens,
        @Nullable BigDecimal costUsd) {}
