package com.javaacademy.platform.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeneratedContent(
        @Valid @NotNull GeneratedLecture lecture,
        @Valid @NotNull GeneratedTask task,
        @NotBlank String templateCode,
        @NotBlank String solutionCode,
        @NotBlank String testCode) {}
