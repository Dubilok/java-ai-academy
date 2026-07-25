package com.javaacademy.platform.ai.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javaacademy.platform.ai.mcp.McpTool;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP tool: compile and run student/AI code against a test suite in the Docker sandbox.
 * Falls back gracefully when the sandbox engine is unavailable (E3 not yet wired up).
 */
@Slf4j
@Component
public class RunInSandboxTool implements McpTool {

    private final Optional<CodeExecutionEngine> executionEngine;
    private final ObjectMapper objectMapper;

    public RunInSandboxTool(Optional<CodeExecutionEngine> executionEngine, ObjectMapper objectMapper) {
        this.executionEngine = executionEngine;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "run_in_sandbox";
    }

    @Override
    public String description() {
        return "Compile and run Java source code against a JUnit 5 test suite in the isolated Docker sandbox. "
                + "Returns PASSED/FAILED/TIMEOUT/ERROR status with logs.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode props = objectMapper.createObjectNode();
        props.set(
                "sourceCode",
                objectMapper
                        .createObjectNode()
                        .put("type", "string")
                        .put("description", "Java source code to compile and test"));
        props.set(
                "testCode",
                objectMapper.createObjectNode().put("type", "string").put("description", "JUnit 5 test class source"));
        props.set(
                "testClassName",
                objectMapper
                        .createObjectNode()
                        .put("type", "string")
                        .put("description", "Simple class name of the test class (default: SolutionTest)"));

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.set("required", objectMapper.createArrayNode().add("sourceCode").add("testCode"));
        schema.set("properties", props);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode input) {
        if (executionEngine.isEmpty()) {
            log.debug("Sandbox execution engine not available; returning NOT_AVAILABLE for MCP run_in_sandbox");
            return objectMapper
                    .createObjectNode()
                    .put("status", "NOT_AVAILABLE")
                    .put("logs", "Sandbox execution engine not configured. Wire up DockerCodeExecutionService (E3).")
                    .put("failedTests", 0)
                    .put("durationMs", 0);
        }

        String sourceCode = input.required("sourceCode").asText();
        String testCode = input.required("testCode").asText();
        String testClassName = input.path("testClassName").asText("SolutionTest");

        ExecutionRequest request = new ExecutionRequest(UUID.randomUUID(), sourceCode, testCode, testClassName);
        ExecutionResult result = executionEngine.get().execute(request);

        return objectMapper
                .createObjectNode()
                .put("status", result.status().name())
                .put("failedTests", result.failedTests())
                .put("logs", result.logs())
                .put("durationMs", result.durationMs());
    }
}
