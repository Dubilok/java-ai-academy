package com.javaacademy.platform.interview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.SecurityConfig;
import com.javaacademy.platform.interview.dto.CreateInterviewQuestionRequest;
import com.javaacademy.platform.interview.dto.InterviewQuestionResponse;
import com.javaacademy.platform.interview.dto.UpdateInterviewQuestionRequest;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.service.FlashcardGeneratorService;
import com.javaacademy.platform.interview.service.InterviewQuestionService;
import java.util.List;
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

@WebMvcTest(InterviewQuestionController.class)
@Import(SecurityConfig.class)
class InterviewQuestionControllerTest {

    static final UUID QUESTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    InterviewQuestionService questionService;

    @MockBean
    FlashcardGeneratorService flashcardGeneratorService;

    @MockBean
    JwtService jwtService;

    // ── GET /interview/questions ───────────────────────────────────────────────

    @Test
    void listQuestions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/interview/questions")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listQuestions_noFilters_returns200WithList() throws Exception {
        InterviewQuestionResponse response = questionResponse();
        when(questionService.findQuestions(null, null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/interview/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(QUESTION_ID.toString()))
                .andExpect(jsonPath("$[0].technology").value("Java"))
                .andExpect(jsonPath("$[0].difficulty").value("INTERMEDIATE"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listQuestions_withFilters_passesFiltersToService() throws Exception {
        when(questionService.findQuestions("Spring", "IoC", InterviewDifficulty.ADVANCED))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/interview/questions")
                        .param("technology", "Spring")
                        .param("category", "IoC")
                        .param("difficulty", "ADVANCED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listQuestions_invalidDifficulty_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/interview/questions").param("difficulty", "INVALID_LEVEL"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /admin/interview/questions ───────────────────────────────────────

    @Test
    void createQuestion_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/interview/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createQuestion_studentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/interview/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createQuestion_adminRole_returns201WithCreatedQuestion() throws Exception {
        when(questionService.create(any())).thenReturn(questionResponse());

        mockMvc.perform(post("/api/v1/admin/interview/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()))
                .andExpect(jsonPath("$.technology").value("Java"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createQuestion_missingTechnology_returns400() throws Exception {
        CreateInterviewQuestionRequest invalidRequest =
                new CreateInterviewQuestionRequest("", "Core", "Question?", null, null, InterviewDifficulty.BEGINNER);

        mockMvc.perform(post("/api/v1/admin/interview/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /admin/interview/questions/{id} ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateQuestion_exists_returns200() throws Exception {
        when(questionService.update(eq(QUESTION_ID), any())).thenReturn(questionResponse());

        mockMvc.perform(put("/api/v1/admin/interview/questions/{id}", QUESTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateQuestion_notFound_returns404() throws Exception {
        when(questionService.update(eq(QUESTION_ID), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(put("/api/v1/admin/interview/questions/{id}", QUESTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /admin/interview/questions/{id} ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteQuestion_exists_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/interview/questions/{id}", QUESTION_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteQuestion_notFound_returns404() throws Exception {
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "Not found"))
                .when(questionService)
                .delete(QUESTION_ID);

        mockMvc.perform(delete("/api/v1/admin/interview/questions/{id}", QUESTION_ID))
                .andExpect(status().isNotFound());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private InterviewQuestionResponse questionResponse() {
        return new InterviewQuestionResponse(
                QUESTION_ID,
                "Java",
                "Core",
                "What is the JVM?",
                "JVM is a runtime.",
                "Detailed explanation.",
                InterviewDifficulty.INTERMEDIATE);
    }

    private CreateInterviewQuestionRequest createRequest() {
        return new CreateInterviewQuestionRequest(
                "Java",
                "Core",
                "What is the JVM?",
                "JVM is a runtime.",
                "Detailed explanation.",
                InterviewDifficulty.INTERMEDIATE);
    }

    private UpdateInterviewQuestionRequest updateRequest() {
        return new UpdateInterviewQuestionRequest(
                "Java", "Core", "What is the JVM?", "Updated short answer.", null, InterviewDifficulty.ADVANCED);
    }
}
