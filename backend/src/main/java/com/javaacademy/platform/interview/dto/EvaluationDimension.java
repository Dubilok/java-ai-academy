package com.javaacademy.platform.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationDimension(
        @NotBlank String name, @NotNull @Min(0) @Max(100) Integer score, @NotBlank String feedback) {}
