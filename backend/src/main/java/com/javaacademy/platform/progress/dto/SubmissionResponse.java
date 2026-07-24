package com.javaacademy.platform.progress.dto;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SubmissionResponse(
        UUID id,
        @Nullable UUID taskId,
        String status,
        @Nullable String logs,
        @Nullable Long durationMs,
        Instant createdAt) {}
