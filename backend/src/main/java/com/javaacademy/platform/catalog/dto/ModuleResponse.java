package com.javaacademy.platform.catalog.dto;

import java.util.List;
import java.util.UUID;

public record ModuleResponse(UUID id, String title, int orderIndex, List<LectureStubResponse> lectures) {}
