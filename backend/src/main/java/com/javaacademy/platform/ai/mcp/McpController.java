package com.javaacademy.platform.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.mcp.dto.McpCallRequest;
import com.javaacademy.platform.ai.mcp.dto.McpCallResult;
import com.javaacademy.platform.ai.mcp.dto.McpToolInfo;
import com.javaacademy.platform.common.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpToolRegistry registry;
    private final ObjectMapper objectMapper;

    @GetMapping("/tools")
    public List<McpToolInfo> listTools() {
        return registry.all().stream()
                .map(tool -> new McpToolInfo(tool.name(), tool.description(), tool.inputSchema()))
                .sorted((firstTool, secondTool) -> firstTool.name().compareTo(secondTool.name()))
                .toList();
    }

    @GetMapping("/tools/{name}")
    public McpToolInfo getTool(@PathVariable String name) {
        McpTool tool =
                registry.find(name).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, toolNotFound(name)));
        return new McpToolInfo(tool.name(), tool.description(), tool.inputSchema());
    }

    @PostMapping("/tools/{name}/call")
    public McpCallResult callTool(@PathVariable String name, @Valid @RequestBody McpCallRequest request) {
        McpTool tool =
                registry.find(name).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, toolNotFound(name)));
        try {
            JsonNode output = tool.execute(request.input());
            return new McpCallResult(name, output, false);
        } catch (ApiException apiException) {
            throw apiException;
        } catch (Exception exception) {
            log.warn("MCP tool '{}' execution failed: {}", name, exception.getMessage());
            return new McpCallResult(name, objectMapper.createObjectNode().put("error", exception.getMessage()), true);
        }
    }

    private static String toolNotFound(String name) {
        return "MCP tool not found: " + name;
    }
}
