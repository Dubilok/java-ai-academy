package com.javaacademy.platform.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserUsageSummary(UUID userId, String email, long requestCount, @Nullable BigDecimal costUsd) {}
