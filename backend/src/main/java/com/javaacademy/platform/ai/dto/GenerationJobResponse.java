package com.javaacademy.platform.ai.dto;

import com.javaacademy.platform.ai.enums.JobStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record GenerationJobResponse(
        UUID jobId,
        String technology,
        JobStatus status,
        int totalItems,
        int completedItems,
        @Nullable String currentItem,
        @Nullable UUID courseId,
        @Nullable String errorMessage) {}
