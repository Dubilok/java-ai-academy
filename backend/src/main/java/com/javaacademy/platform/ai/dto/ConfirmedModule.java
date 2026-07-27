package com.javaacademy.platform.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConfirmedModule(
        @NotBlank @Size(max = 300) String moduleName,
        @NotEmpty @Valid List<ConfirmedLecture> lectures) {}
