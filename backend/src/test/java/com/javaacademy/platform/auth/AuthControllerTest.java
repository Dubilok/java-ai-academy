package com.javaacademy.platform.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;

    @Test
    void register_withValidBody_returns201WithTokens() throws Exception {
        given(authService.register(any())).willReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", "securePassword1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("not-an-email", "securePassword1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withPasswordTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("user@example.com", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"securePassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        given(authService.register(any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("dup@example.com", "securePassword1"))))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withValidCredentials_returns200WithTokens() throws Exception {
        given(authService.login(any())).willReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "securePassword1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        given(authService.login(any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(new LoginRequest("user@example.com", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBlankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withMissingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── /auth/refresh ─────────────────────────────────────────────────────────

    @Test
    void refresh_withValidToken_returns200WithNewTokenPair() throws Exception {
        given(authService.refresh(any())).willReturn(new AuthResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_withBlankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withMissingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withRevokedToken_returns401() throws Exception {
        given(authService.refresh(any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"reused-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withExpiredToken_returns401() throws Exception {
        given(authService.refresh(any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired-token\"}"))
                .andExpect(status().isUnauthorized());
    }
}
