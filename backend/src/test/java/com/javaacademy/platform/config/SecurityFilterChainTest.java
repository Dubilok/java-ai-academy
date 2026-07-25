package com.javaacademy.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javaacademy.platform.ai.service.GenerationJobService;
import com.javaacademy.platform.ai.service.HintService;
import com.javaacademy.platform.auth.dto.AuthResponse;
import com.javaacademy.platform.auth.service.AuthService;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.catalog.service.CatalogService;
import com.javaacademy.platform.interview.service.InterviewQuestionService;
import com.javaacademy.platform.interview.service.InterviewSessionService;
import com.javaacademy.platform.progress.service.MeService;
import com.javaacademy.platform.progress.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityFilterChainTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JwtService jwtService;

    @MockBean
    AuthService authService;

    @MockBean
    CatalogService catalogService;

    @MockBean
    MeService meService;

    @MockBean
    SubmissionService submissionService;

    @MockBean
    GenerationJobService generationJobService;

    @MockBean
    HintService hintService;

    @MockBean
    InterviewQuestionService questionService;

    @MockBean
    InterviewSessionService sessionService;

    @MockBean
    com.javaacademy.platform.ai.service.FinOpsService finOpsService;

    @MockBean
    com.javaacademy.platform.ai.mcp.McpToolRegistry mcpToolRegistry;

    @MockBean
    com.javaacademy.platform.ai.service.EvaluationDashboardService evaluationDashboardService;

    // ── public paths ───────────────────────────────────────────────────────────

    @Test
    void authRegisterEndpoint_withNoToken_isNotBlocked() throws Exception {
        given(authService.register(anyObject())).willReturn(new AuthResponse("access", "refresh"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(isNotSecurityError());
    }

    @Test
    void authLoginEndpoint_withNoToken_isNotBlocked() throws Exception {
        given(authService.login(anyObject())).willReturn(new AuthResponse("access", "refresh"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(isNotSecurityError());
    }

    // ── unauthenticated requests on protected paths ────────────────────────────

    @Test
    void protectedEndpoint_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withInvalidJwt_returns401() throws Exception {
        given(jwtService.isTokenValid("bad.token")).willReturn(false);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }

    // ── role-based authorisation rules ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "STUDENT")
    void adminEndpoint_withStudentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/usage")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_withAdminRole_passesSecurityAndReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/usage")).andExpect(isNotSecurityError());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentEndpoint_withStudentRole_passesSecurityAndReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(isNotSecurityError());
    }

    // ── JWT filter integration ─────────────────────────────────────────────────

    @Test
    void requestWithValidStudentJwt_isAuthenticatedAndPassesSecurity() throws Exception {
        given(jwtService.isTokenValid(anyString())).willReturn(true);
        given(jwtService.extractEmail(anyString())).willReturn("user@example.com");
        given(jwtService.extractRole(anyString())).willReturn("ROLE_STUDENT");

        // Security context populated by JWT filter → passes security → MeController handles it
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer student.jwt.token"))
                .andExpect(isNotSecurityError());
    }

    @Test
    void requestWithValidAdminJwt_canAccessAdminPaths() throws Exception {
        given(jwtService.isTokenValid(anyString())).willReturn(true);
        given(jwtService.extractEmail(anyString())).willReturn("admin@example.com");
        given(jwtService.extractRole(anyString())).willReturn("ROLE_ADMIN");

        // Admin JWT → ROLE_ADMIN authority → admin path passes → controller handles it
        mockMvc.perform(get("/api/v1/admin/ai/usage").header("Authorization", "Bearer admin.jwt.token"))
                .andExpect(isNotSecurityError());
    }

    @Test
    void requestWithStudentJwt_cannotAccessAdminPaths() throws Exception {
        given(jwtService.isTokenValid(anyString())).willReturn(true);
        given(jwtService.extractEmail(anyString())).willReturn("user@example.com");
        given(jwtService.extractRole(anyString())).willReturn("ROLE_STUDENT");

        mockMvc.perform(get("/api/v1/admin/ai/usage").header("Authorization", "Bearer student.jwt.token"))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Asserts the response is NOT a security rejection (not 401, not 403). */
    private static ResultMatcher isNotSecurityError() {
        return result -> {
            int status = result.getResponse().getStatus();
            assertThat(status)
                    .as("Expected no security rejection but got %d", status)
                    .isNotIn(401, 403);
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T anyObject() {
        return (T) org.mockito.ArgumentMatchers.any();
    }
}
