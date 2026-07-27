package com.javaacademy.platform.catalog.dto;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** testCode and solutionCode are deliberately excluded — never sent to a student client. */
public record TaskResponse(
        UUID id,
        UUID courseId,
        String title,
        @Nullable String description,
        String difficulty,
        @Nullable String templateCode,
        long xpReward) {}
