package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProposeLecturesRequest(
        @NotBlank @Size(max = 100) String technology,
        @NotBlank @Size(max = 300) String moduleName,
        List<String> existingLectures) {}
