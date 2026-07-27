package com.javaacademy.platform.catalog.dto;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record LectureResponse(
        UUID id,
        UUID courseId,
        String title,
        @Nullable String contentMarkdown,
        int orderIndex,
        List<TaskStubResponse> tasks) {}
