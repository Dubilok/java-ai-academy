package com.javaacademy.platform.ai.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javaacademy.platform.ai.mcp.McpTool;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * MCP tool: look up a task by ID and return its full content (including testCode and solutionCode).
 * This is an admin/AI-internal tool — testCode must never be exposed via student-facing endpoints.
 */
@Component
@RequiredArgsConstructor
public class LookupTaskTool implements McpTool {

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "lookup_task";
    }

    @Override
    public String description() {
        return "Look up a task by UUID to retrieve its full description, template code, test code, and solution code. "
                + "For internal Content Architect use only — never expose testCode/solutionCode in student responses.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode taskIdProp = objectMapper
                .createObjectNode()
                .put("type", "string")
                .put("format", "uuid")
                .put("description", "UUID of the task to retrieve");

        ObjectNode props = objectMapper.createObjectNode();
        props.set("taskId", taskIdProp);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.set("required", objectMapper.createArrayNode().add("taskId"));
        schema.set("properties", props);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String rawId = input.required("taskId").asText();
        UUID taskId;
        try {
            taskId = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid taskId UUID: " + rawId);
        }

        Task task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", task.getId().toString());
        result.put("title", task.getTitle());
        if (task.getDescription() != null) result.put("description", task.getDescription());
        result.put("difficulty", task.getDifficulty());
        if (task.getTemplateCode() != null) result.put("templateCode", task.getTemplateCode());
        if (task.getTestCode() != null) result.put("testCode", task.getTestCode());
        if (task.getSolutionCode() != null) result.put("solutionCode", task.getSolutionCode());
        result.put("xpReward", task.getXpReward());
        return result;
    }
}
