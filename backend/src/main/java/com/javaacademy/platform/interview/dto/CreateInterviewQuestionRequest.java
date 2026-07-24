package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record CreateInterviewQuestionRequest(
        @NotBlank String technology,
        @NotBlank String category,
        @NotBlank String question,
        @Nullable String shortAnswer,
        @Nullable String detailedExplanation,
        @NotNull InterviewDifficulty difficulty) {}
