package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record GenerateFlashcardsRequest(
        @NotBlank @Size(max = 100) String technology,
        @Nullable @Size(max = 100) String category,
        @Min(1) @Max(20) int count,
        @Nullable InterviewDifficulty difficulty) {}
