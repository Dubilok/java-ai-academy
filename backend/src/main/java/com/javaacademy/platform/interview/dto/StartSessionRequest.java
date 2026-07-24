package com.javaacademy.platform.interview.dto;

import jakarta.validation.constraints.NotBlank;

public record StartSessionRequest(@NotBlank String technology) {}
