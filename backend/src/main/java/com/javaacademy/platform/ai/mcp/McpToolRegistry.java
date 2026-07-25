package com.javaacademy.platform.ai.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Holds all registered MCP tools, keyed by name. */
@Component
public class McpToolRegistry {

    private final Map<String, McpTool> toolsByName;

    public McpToolRegistry(List<McpTool> tools) {
        toolsByName = tools.stream().collect(Collectors.toUnmodifiableMap(McpTool::name, Function.identity()));
    }

    public Collection<McpTool> all() {
        return toolsByName.values();
    }

    public Optional<McpTool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }
}
