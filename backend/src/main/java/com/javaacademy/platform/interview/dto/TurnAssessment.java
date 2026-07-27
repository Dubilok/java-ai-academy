package com.javaacademy.platform.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TurnAssessment(@NotBlank String topicCovered, @NotNull String strength, @NotBlank String note) {}
