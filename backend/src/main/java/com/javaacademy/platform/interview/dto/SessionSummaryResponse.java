package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SessionSummaryResponse(
        UUID sessionId, String technology, InterviewSessionStatus status, @Nullable Integer score, Instant createdAt) {}
