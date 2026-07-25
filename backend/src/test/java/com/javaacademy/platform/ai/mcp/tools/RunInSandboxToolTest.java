package com.javaacademy.platform.ai.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class RunInSandboxToolTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void execute_withoutEngine_returnsNotAvailable() {
        RunInSandboxTool tool = new RunInSandboxTool(Optional.empty(), objectMapper);

        JsonNode input =
                objectMapper.createObjectNode().put("sourceCode", "class S{}").put("testCode", "@Test void t(){}");

        JsonNode result = tool.execute(input);

        assertThat(result.get("status").asText()).isEqualTo("NOT_AVAILABLE");
    }

    @Test
    @SneakyThrows
    void execute_withEngine_passedResult_returnsPassed() {
        CodeExecutionEngine engine = mock(CodeExecutionEngine.class);
        when(engine.execute(any())).thenReturn(ExecutionResult.passed("All tests passed", 350L));
        RunInSandboxTool tool = new RunInSandboxTool(Optional.of(engine), objectMapper);

        JsonNode input = objectMapper
                .createObjectNode()
                .put("sourceCode", "class Solution{}")
                .put("testCode", "@Test void t(){}")
                .put("testClassName", "SolutionTest");

        JsonNode result = tool.execute(input);

        assertThat(result.get("status").asText()).isEqualTo("PASSED");
        assertThat(result.get("failedTests").asInt()).isZero();
        assertThat(result.get("logs").asText()).isEqualTo("All tests passed");
        assertThat(result.get("durationMs").asLong()).isEqualTo(350L);
    }

    @Test
    @SneakyThrows
    void execute_withEngine_failedResult_returnsFailed() {
        CodeExecutionEngine engine = mock(CodeExecutionEngine.class);
        when(engine.execute(any())).thenReturn(ExecutionResult.failed(2, "2 tests failed", 200L));
        RunInSandboxTool tool = new RunInSandboxTool(Optional.of(engine), objectMapper);

        JsonNode input = objectMapper
                .createObjectNode()
                .put("sourceCode", "class Solution{}")
                .put("testCode", "@Test void t(){}");

        JsonNode result = tool.execute(input);

        assertThat(result.get("status").asText()).isEqualTo("FAILED");
        assertThat(result.get("failedTests").asInt()).isEqualTo(2);
    }

    @Test
    @SneakyThrows
    void execute_withEngine_usesDefaultTestClassName() {
        CodeExecutionEngine engine = mock(CodeExecutionEngine.class);
        when(engine.execute(any())).thenReturn(new ExecutionResult(ExecutionStatus.PASSED, 0, "", 0L));
        RunInSandboxTool tool = new RunInSandboxTool(Optional.of(engine), objectMapper);

        JsonNode input = objectMapper
                .createObjectNode()
                .put("sourceCode", "class Solution{}")
                .put("testCode", "@Test void t(){}");

        tool.execute(input);

        var captor = org.mockito.ArgumentCaptor.forClass(com.javaacademy.platform.sandbox.dto.ExecutionRequest.class);
        verify(engine).execute(captor.capture());
        assertThat(captor.getValue().testClassName()).isEqualTo("SolutionTest");
    }

    @Test
    void name_returnsRunInSandbox() {
        RunInSandboxTool tool = new RunInSandboxTool(Optional.empty(), objectMapper);
        assertThat(tool.name()).isEqualTo("run_in_sandbox");
    }

    @Test
    void inputSchema_hasRequiredFields() {
        RunInSandboxTool tool = new RunInSandboxTool(Optional.empty(), objectMapper);
        JsonNode schema = tool.inputSchema();

        assertThat(schema.get("required").toString()).contains("sourceCode").contains("testCode");
        assertThat(schema.get("properties").has("sourceCode")).isTrue();
        assertThat(schema.get("properties").has("testCode")).isTrue();
        assertThat(schema.get("properties").has("testClassName")).isTrue();
    }
}
