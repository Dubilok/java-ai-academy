package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ImportContentRequest(@NotNull UUID moduleId, @NotBlank String rawJson) {}
