package com.javaacademy.platform.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javaacademy.platform.ai.dto.HintResponse;
import com.javaacademy.platform.ai.service.HintService;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HintController.class)
@Import(SecurityConfig.class)
class HintControllerTest {

    static final UUID TASK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    MockMvc mockMvc;

    @MockBean
    HintService hintService;

    @MockBean
    JwtService jwtService;

    @Test
    void requestHint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-hint", TASK_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void requestHint_authenticated_returns200WithHint() throws Exception {
        HintResponse response = new HintResponse(TASK_ID, "What does the error mean?");
        when(hintService.generateHint(eq(TASK_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-hint", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.hint").value("What does the error mean?"));
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void requestHint_taskNotFound_returns404() throws Exception {
        when(hintService.generateHint(eq(TASK_ID), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-hint", TASK_ID)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void requestHint_rateLimitExceeded_returns429() throws Exception {
        when(hintService.generateHint(eq(TASK_ID), any()))
                .thenThrow(new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-hint", TASK_ID)).andExpect(status().isTooManyRequests());
    }
}
