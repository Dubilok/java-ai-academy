package com.javaacademy.platform.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CourseUsageSummary(UUID courseId, String title, long requestCount, @Nullable BigDecimal costUsd) {}
