package com.javaacademy.platform.ai.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.dto.AiUsageResponse;
import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.ai.service.FinOpsService;
import com.javaacademy.platform.ai.service.GenerationJobService;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.config.SecurityConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAiController.class)
@Import(SecurityConfig.class)
class AdminAiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    GenerationJobService generationJobService;

    @MockBean
    FinOpsService finOpsService;

    @MockBean
    JwtService jwtService;

    // ── POST /admin/ai/generate-course ─────────────────────────────────────────

    @Test
    void startGeneration_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/generate-course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technology\":\"Java Records\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void startGeneration_studentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/generate-course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technology\":\"Java Records\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void startGeneration_adminRole_returns202WithJobId() throws Exception {
        UUID jobId = UUID.randomUUID();
        given(generationJobService.startJob(eq("Java Records"))).willReturn(jobId);

        mockMvc.perform(post("/api/v1/admin/ai/generate-course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technology\":\"Java Records\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.technology").value("Java Records"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void startGeneration_blankTechnology_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/generate-course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technology\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /admin/ai/jobs/{id} ────────────────────────────────────────────────

    @Test
    void getJob_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/jobs/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getJob_unknownId_returns404() throws Exception {
        UUID jobId = UUID.randomUUID();
        given(generationJobService.getJob(jobId)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/ai/jobs/" + jobId)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getJob_existingRunningJob_returnsJobStatus() throws Exception {
        UUID jobId = UUID.randomUUID();
        GenerationJobResponse runningResponse =
                new GenerationJobResponse(jobId, "Java Records", JobStatus.RUNNING, null, null);
        given(generationJobService.getJob(jobId)).willReturn(Optional.of(runningResponse));

        mockMvc.perform(get("/api/v1/admin/ai/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getJob_succeededJob_returnsCourseId() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        GenerationJobResponse succeededResponse =
                new GenerationJobResponse(jobId, "Java Records", JobStatus.SUCCEEDED, courseId, null);
        given(generationJobService.getJob(jobId)).willReturn(Optional.of(succeededResponse));

        mockMvc.perform(get("/api/v1/admin/ai/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.courseId").value(courseId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getJob_failedJob_returnsErrorMessage() throws Exception {
        UUID jobId = UUID.randomUUID();
        GenerationJobResponse failedResponse = new GenerationJobResponse(
                jobId, "Java Records", JobStatus.FAILED, null, "Content generation failed after 3 attempts");
        given(generationJobService.getJob(jobId)).willReturn(Optional.of(failedResponse));

        mockMvc.perform(get("/api/v1/admin/ai/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Content generation failed after 3 attempts"));
    }

    // ── GET /admin/ai/usage ────────────────────────────────────────────────────

    @Test
    void getUsage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/usage")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getUsage_studentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/usage")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsage_noFilter_returnsAggregatedStats() throws Exception {
        AiUsageResponse emptyUsage = new AiUsageResponse(null, null, 0L, 0L, 0L, null, List.of(), List.of(), List.of());
        given(finOpsService.getUsage(null, null)).willReturn(emptyUsage);

        mockMvc.perform(get("/api/v1/admin/ai/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(0))
                .andExpect(jsonPath("$.byAgent").isArray())
                .andExpect(jsonPath("$.byUser").isArray())
                .andExpect(jsonPath("$.byCourse").isArray());
    }
}
