package com.javaacademy.platform.sandbox.dto;

import com.javaacademy.platform.sandbox.enums.ExecutionStatus;

public record ExecutionResult(ExecutionStatus status, String logs, long durationMs) {

    public static ExecutionResult passed(String logs, long durationMs) {
        return new ExecutionResult(ExecutionStatus.PASSED, logs, durationMs);
    }

    public static ExecutionResult failed(String logs, long durationMs) {
        return new ExecutionResult(ExecutionStatus.FAILED, logs, durationMs);
    }

    public static ExecutionResult timeout(String message) {
        return new ExecutionResult(ExecutionStatus.TIMEOUT, message, 0L);
    }

    public static ExecutionResult error(String message) {
        return new ExecutionResult(ExecutionStatus.ERROR, message, 0L);
    }
}
