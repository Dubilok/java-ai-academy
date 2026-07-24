package com.javaacademy.platform.catalog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CourseDetailResponse(
        UUID id,
        String title,
        @Nullable String description,
        String technology,
        Instant createdAt,
        List<ModuleResponse> modules) {}
