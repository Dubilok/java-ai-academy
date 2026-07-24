package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateCourseRequest(@NotBlank @Size(max = 100) String technology) {}
