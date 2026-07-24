package com.javaacademy.platform.progress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitCodeRequest(
        @NotBlank(message = "Source code must not be blank")
                @Size(max = 65_536, message = "Source code must not exceed 64 KB")
                String source) {}
