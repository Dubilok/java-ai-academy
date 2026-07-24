package com.javaacademy.platform.ai.dto;

import com.javaacademy.platform.catalog.enums.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GeneratedTask(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(min = 30) String description,
        @NotNull Difficulty difficulty,
        @Min(1) @Max(1000) int xpReward) {}
