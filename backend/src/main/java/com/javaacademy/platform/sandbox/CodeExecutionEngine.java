package com.javaacademy.platform.sandbox;

import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;

public interface CodeExecutionEngine {

    ExecutionResult execute(ExecutionRequest request);
}
