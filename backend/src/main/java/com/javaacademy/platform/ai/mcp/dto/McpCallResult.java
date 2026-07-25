package com.javaacademy.platform.ai.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record McpCallResult(String toolName, JsonNode output, boolean isError) {}
