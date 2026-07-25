package com.javaacademy.platform.ai.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javaacademy.platform.ai.mcp.McpTool;
import com.javaacademy.platform.common.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * MCP tool: fetch a bundled specification document by name.
 * Documents live in {@code classpath:mcp-specs/}; the allowed-name set is the security boundary.
 */
@Component
@RequiredArgsConstructor
public class FetchSpecTool implements McpTool {

    static final Set<String> ALLOWED_SPECS = Set.of("api-contract", "project-overview");

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "fetch_spec";
    }

    @Override
    public String description() {
        return "Fetch a platform specification document by name. Available: "
                + String.join(", ", ALLOWED_SPECS)
                + ". Returns the document content as a string.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode nameProp = objectMapper
                .createObjectNode()
                .put("type", "string")
                .put("description", "Spec document name. Allowed values: " + String.join(", ", ALLOWED_SPECS));

        ObjectNode props = objectMapper.createObjectNode();
        props.set("name", nameProp);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.set("required", objectMapper.createArrayNode().add("name"));
        schema.set("properties", props);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode input) throws IOException {
        String specName = input.required("name").asText();
        if (!ALLOWED_SPECS.contains(specName)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Unknown spec '" + specName + "'. Allowed: " + ALLOWED_SPECS);
        }

        Resource resource = resourceLoader.getResource("classpath:mcp-specs/" + specName + ".md");
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Spec file not found on classpath: " + specName);
        }

        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.createObjectNode().put("name", specName).put("content", content);
    }
}
