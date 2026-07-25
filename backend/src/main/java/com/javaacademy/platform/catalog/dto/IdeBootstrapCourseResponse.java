package com.javaacademy.platform.catalog.dto;

import java.util.List;
import java.util.UUID;

/** Trimmed course representation for the IntelliJ plugin tool window. */
public record IdeBootstrapCourseResponse(
        UUID id, String title, String technology, List<IdeBootstrapTaskResponse> tasks) {}
