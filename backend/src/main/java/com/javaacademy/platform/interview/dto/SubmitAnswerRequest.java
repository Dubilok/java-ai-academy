package com.javaacademy.platform.interview.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitAnswerRequest(@NotBlank String answerText) {}
