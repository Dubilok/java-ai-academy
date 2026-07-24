package com.javaacademy.platform.catalog.dto;

import java.util.UUID;

public record LectureStubResponse(UUID id, String title, int orderIndex) {}
