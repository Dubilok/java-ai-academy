package com.javaacademy.platform.interview.dto;

import com.javaacademy.platform.interview.enums.InterviewMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record StartSessionRequest(
        @NotBlank String technology, @Nullable InterviewMode mode, @Nullable @Min(1) @Max(30) Integer maxTurns) {}
