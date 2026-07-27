package com.javaacademy.platform.interview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitVoiceAnswerRequest(
        @NotBlank String transcript, @NotNull @Min(0) Integer turnIndex, @NotNull @Min(0) Integer durationSeconds) {}
