package com.javaacademy.platform.sandbox.dto;

import com.javaacademy.platform.sandbox.enums.ExecutionStatus;

public record ExecutionResult(ExecutionStatus status, int failedTests, String logs, long durationMs) {

    public static ExecutionResult passed(String logs, long durationMs) {
        return new ExecutionResult(ExecutionStatus.PASSED, 0, logs, durationMs);
    }

    public static ExecutionResult failed(int failedTests, String logs, long durationMs) {
        return new ExecutionResult(ExecutionStatus.FAILED, failedTests, logs, durationMs);
    }

    public static ExecutionResult timeout(String message) {
        return new ExecutionResult(ExecutionStatus.TIMEOUT, 0, message, 0L);
    }

    public static ExecutionResult error(String message) {
        return new ExecutionResult(ExecutionStatus.ERROR, 0, message, 0L);
    }
}
