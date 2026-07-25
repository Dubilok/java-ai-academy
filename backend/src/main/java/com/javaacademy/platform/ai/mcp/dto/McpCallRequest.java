package com.javaacademy.platform.ai.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record McpCallRequest(@NotNull JsonNode input) {}
