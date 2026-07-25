package com.javaacademy.platform.ai.dto;

import com.javaacademy.platform.ai.enums.AgentType;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record AgentUsageSummary(
        AgentType agent,
        long requestCount,
        long promptTokens,
        long completionTokens,
        @Nullable BigDecimal costUsd,
        @Nullable Long avgLatencyMs) {}
