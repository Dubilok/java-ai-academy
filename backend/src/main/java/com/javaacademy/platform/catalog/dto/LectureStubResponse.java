package com.javaacademy.platform.catalog.dto;

import java.util.List;
import java.util.UUID;

public record LectureStubResponse(UUID id, String title, int orderIndex, List<TaskStubResponse> tasks) {}
