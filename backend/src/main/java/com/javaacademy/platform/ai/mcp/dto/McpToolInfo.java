package com.javaacademy.platform.ai.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record McpToolInfo(String name, String description, JsonNode inputSchema) {}
