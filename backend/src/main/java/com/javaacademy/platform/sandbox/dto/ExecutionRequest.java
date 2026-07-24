package com.javaacademy.platform.sandbox.dto;

import java.util.UUID;

public record ExecutionRequest(UUID taskId, String solutionCode, String testCode, String testClassName) {}
