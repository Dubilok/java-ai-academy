package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmedLecture(@NotBlank @Size(max = 300) String lectureTitle, @Min(1) @Max(10) int taskCount) {}
