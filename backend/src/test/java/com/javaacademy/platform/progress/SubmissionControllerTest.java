package com.javaacademy.platform.progress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.SecurityConfig;
import com.javaacademy.platform.progress.controller.SubmissionController;
import com.javaacademy.platform.progress.dto.SubmissionResponse;
import com.javaacademy.platform.progress.dto.SubmitCodeRequest;
import com.javaacademy.platform.progress.dto.SubmitResponse;
import com.javaacademy.platform.progress.service.SubmissionService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubmissionController.class)
@Import(SecurityConfig.class)
class SubmissionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SubmissionService submissionService;

    @MockBean
    JwtService jwtService;

    // ── POST /tasks/{taskId}/submissions ──────────────────────────────────────

    @Test
    @WithMockUser(username = "student@test.com")
    void submitCode_validRequest_returns202WithSubmissionId() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        given(submissionService.createSubmission(eq("student@test.com"), eq(taskId), any()))
                .willReturn(new SubmitResponse(submissionId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitCodeRequest("public class Solution {}"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.submissionId").value(submissionId.toString()));
    }

    @Test
    void submitCode_unauthenticated_returns401() throws Exception {
        UUID taskId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitCodeRequest("public class Solution {}"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void submitCode_blankSource_returns400() throws Exception {
        UUID taskId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitCodeRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void submitCode_sourceTooLong_returns400() throws Exception {
        UUID taskId = UUID.randomUUID();
        String oversizedSource = "x".repeat(65_537);
        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitCodeRequest(oversizedSource))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void submitCode_taskNotFound_returns404() throws Exception {
        UUID taskId = UUID.randomUUID();
        given(submissionService.createSubmission(any(), eq(taskId), any()))
                .willThrow(new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitCodeRequest("public class Solution {}"))))
                .andExpect(status().isNotFound());
    }

    // ── GET /submissions/{submissionId} ───────────────────────────────────────

    @Test
    @WithMockUser(username = "student@test.com")
    void getSubmission_ownedByUser_returns200WithDetails() throws Exception {
        UUID submissionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        SubmissionResponse response = new SubmissionResponse(
                submissionId, taskId, "PASSED", "All tests passed", 1234L, Instant.parse("2026-07-24T10:00:00Z"));
        given(submissionService.findSubmission(eq(submissionId), eq("student@test.com")))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/submissions/{submissionId}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId.toString()))
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.logs").value("All tests passed"))
                .andExpect(jsonPath("$.durationMs").value(1234));
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void getSubmission_pendingSubmission_returns200WithNullLogsAndDuration() throws Exception {
        UUID submissionId = UUID.randomUUID();
        SubmissionResponse response = new SubmissionResponse(
                submissionId, null, "PENDING", null, null, Instant.parse("2026-07-24T10:00:00Z"));
        given(submissionService.findSubmission(eq(submissionId), eq("student@test.com")))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/submissions/{submissionId}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.logs").doesNotExist());
    }

    @Test
    void getSubmission_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/{submissionId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void getSubmission_notFoundOrNotOwned_returns404() throws Exception {
        UUID submissionId = UUID.randomUUID();
        given(submissionService.findSubmission(eq(submissionId), eq("student@test.com")))
                .willThrow(new ApiException(HttpStatus.NOT_FOUND, "Submission not found: " + submissionId));

        mockMvc.perform(get("/api/v1/submissions/{submissionId}", submissionId)).andExpect(status().isNotFound());
    }
}
