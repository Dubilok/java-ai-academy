package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record InterviewQuestionResponse(
        UUID id,
        String technology,
        String category,
        String question,
        @Nullable String shortAnswer,
        @Nullable String detailedExplanation,
        InterviewDifficulty difficulty) {}
