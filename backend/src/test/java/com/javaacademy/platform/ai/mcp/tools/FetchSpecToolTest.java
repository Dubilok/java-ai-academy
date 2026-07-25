package com.javaacademy.platform.ai.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.common.ApiException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class FetchSpecToolTest {

    FetchSpecTool tool;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tool = new FetchSpecTool(new DefaultResourceLoader(), objectMapper);
    }

    @Test
    @SneakyThrows
    void execute_apiContractSpec_returnsContent() {
        JsonNode input = objectMapper.createObjectNode().put("name", "api-contract");
        JsonNode result = tool.execute(input);

        assertThat(result.get("name").asText()).isEqualTo("api-contract");
        assertThat(result.get("content").asText()).contains("Base path");
    }

    @Test
    @SneakyThrows
    void execute_projectOverviewSpec_returnsContent() {
        JsonNode input = objectMapper.createObjectNode().put("name", "project-overview");
        JsonNode result = tool.execute(input);

        assertThat(result.get("name").asText()).isEqualTo("project-overview");
        assertThat(result.get("content").asText()).isNotBlank();
    }

    @Test
    @SneakyThrows
    void execute_unknownSpec_throwsApiException() {
        JsonNode input = objectMapper.createObjectNode().put("name", "../../secrets");

        assertThatThrownBy(() -> tool.execute(input))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown spec");
    }

    @Test
    @SneakyThrows
    void execute_pathTraversalAttempt_rejected() {
        JsonNode input = objectMapper.createObjectNode().put("name", "../application");

        assertThatThrownBy(() -> tool.execute(input))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown spec");
    }

    @Test
    void name_returnsFetchSpec() {
        assertThat(tool.name()).isEqualTo("fetch_spec");
    }

    @Test
    void inputSchema_hasRequiredName() {
        JsonNode schema = tool.inputSchema();

        assertThat(schema.get("type").asText()).isEqualTo("object");
        assertThat(schema.get("required").get(0).asText()).isEqualTo("name");
    }

    @Test
    void allowedSpecs_containsExpectedValues() {
        assertThat(FetchSpecTool.ALLOWED_SPECS).contains("api-contract", "project-overview");
    }
}
