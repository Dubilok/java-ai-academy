package com.javaacademy.platform.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConfirmedCurriculum(
        @NotBlank @Size(max = 120) String courseName,
        @NotBlank @Size(max = 500) String description,
        @NotEmpty @Valid List<ConfirmedModule> modules) {}
