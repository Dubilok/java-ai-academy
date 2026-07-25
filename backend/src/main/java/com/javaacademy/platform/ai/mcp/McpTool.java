package com.javaacademy.platform.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/** An MCP (Model Context Protocol) tool that the Content Architect agent can invoke. */
public interface McpTool {

    String name();

    String description();

    /** JSON Schema (as a parsed JsonNode) describing the tool's input object. */
    JsonNode inputSchema();

    /**
     * Execute the tool with the given input.
     *
     * @throws Exception on validation failure, not-found, or execution error — callers map to McpCallResult(isError=true)
     */
    JsonNode execute(JsonNode input) throws Exception;
}
