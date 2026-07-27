package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProposeModulesRequest(
        @NotBlank @Size(max = 100) String technology, List<String> existingModules) {}
