package com.javaacademy.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurriculumProposalRequest(@NotBlank @Size(max = 100) String technology) {}
