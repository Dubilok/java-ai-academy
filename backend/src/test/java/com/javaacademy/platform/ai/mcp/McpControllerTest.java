package com.javaacademy.platform.ai.mcp;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.config.SecurityConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(McpController.class)
@Import(SecurityConfig.class)
class McpControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    McpToolRegistry registry;

    @MockBean
    JwtService jwtService;

    // ── GET /admin/mcp/tools ────────────────────────────────────────────────────

    @Test
    void listTools_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/mcp/tools")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listTools_studentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/mcp/tools")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listTools_adminRole_returnsToolList() throws Exception {
        McpTool fakeTool = buildFakeTool("lookup_task", "Look up a task by ID");
        given(registry.all()).willReturn(List.of(fakeTool));

        mockMvc.perform(get("/api/v1/admin/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("lookup_task"))
                .andExpect(jsonPath("$[0].description").value("Look up a task by ID"));
    }

    // ── GET /admin/mcp/tools/{name} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTool_knownTool_returnsToolInfo() throws Exception {
        McpTool fakeTool = buildFakeTool("fetch_spec", "Fetch a spec");
        given(registry.find("fetch_spec")).willReturn(Optional.of(fakeTool));

        mockMvc.perform(get("/api/v1/admin/mcp/tools/fetch_spec"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("fetch_spec"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTool_unknownTool_returns404() throws Exception {
        given(registry.find("nonexistent")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/mcp/tools/nonexistent")).andExpect(status().isNotFound());
    }

    // ── POST /admin/mcp/tools/{name}/call ───────────────────────────────────────

    @Test
    void callTool_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/mcp/tools/lookup_task/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":{}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void callTool_studentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/mcp/tools/lookup_task/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":{}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void callTool_adminRole_validInput_returnsResult() throws Exception {
        McpTool fakeTool = buildFakeTool("lookup_task", "Look up a task");
        given(registry.find("lookup_task")).willReturn(Optional.of(fakeTool));

        mockMvc.perform(post("/api/v1/admin/mcp/tools/lookup_task/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":{\"taskId\":\"00000000-0000-0000-0000-000000000001\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolName").value("lookup_task"))
                .andExpect(jsonPath("$.isError").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void callTool_unknownTool_returns404() throws Exception {
        given(registry.find("ghost")).willReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/admin/mcp/tools/ghost/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":{}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void callTool_missingInputField_returns400() throws Exception {
        // JSON null maps to NullNode (not Java null), so omitting the field triggers @NotNull
        mockMvc.perform(post("/api/v1/admin/mcp/tools/lookup_task/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── helper ──────────────────────────────────────────────────────────────────

    private McpTool buildFakeTool(String name, String description) {
        return new McpTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode inputSchema() {
                return objectMapper.createObjectNode().put("type", "object");
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode execute(com.fasterxml.jackson.databind.JsonNode input) {
                return objectMapper.createObjectNode().put("result", "ok");
            }
        };
    }
}
