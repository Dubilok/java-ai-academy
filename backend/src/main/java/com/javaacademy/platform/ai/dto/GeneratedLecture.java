package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneratedLecture(
        @NotBlank @Size(max = 255) String title, @NotBlank @Size(min = 100) String contentMarkdown) {}
