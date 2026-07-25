package com.javaacademy.platform.interview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.SecurityConfig;
import com.javaacademy.platform.interview.dto.EvaluationDimension;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import com.javaacademy.platform.interview.dto.FinishSessionResponse;
import com.javaacademy.platform.interview.dto.QuestionInSession;
import com.javaacademy.platform.interview.dto.SessionDetailResponse;
import com.javaacademy.platform.interview.dto.SessionSummaryResponse;
import com.javaacademy.platform.interview.dto.StartSessionRequest;
import com.javaacademy.platform.interview.dto.StartSessionResponse;
import com.javaacademy.platform.interview.dto.SubmitAnswerRequest;
import com.javaacademy.platform.interview.dto.SubmitAnswerResponse;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import com.javaacademy.platform.interview.service.InterviewSessionService;
import java.time.Instant;
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

@WebMvcTest(InterviewSessionController.class)
@Import(SecurityConfig.class)
class InterviewSessionControllerTest {

    static final UUID SESSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID QUESTION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    static final UUID ANSWER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    InterviewSessionService sessionService;

    @MockBean
    JwtService jwtService;

    // ── POST /interview/sessions ───────────────────────────────────────────────

    @Test
    void startSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest("Java"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void startSession_validRequest_returns201WithFirstQuestion() throws Exception {
        QuestionInSession firstQuestion =
                new QuestionInSession(QUESTION_ID, "What is the JVM?", "Core", InterviewDifficulty.BEGINNER);
        StartSessionResponse response =
                new StartSessionResponse(SESSION_ID, "Java", InterviewSessionStatus.ACTIVE, firstQuestion);
        when(sessionService.startSession(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest("Java"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.firstQuestion.questionText").value("What is the JVM?"));
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void startSession_blankTechnology_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void startSession_noQuestionsForTechnology_returns422() throws Exception {
        when(sessionService.startSession(any(), any()))
                .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No questions found"));

        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest("Cobol"))))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /interview/sessions/{id}/answers ──────────────────────────────────

    @Test
    void submitAnswer_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest("My answer"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitAnswer_validAnswer_returns200WithNextQuestion() throws Exception {
        QuestionInSession nextQuestion =
                new QuestionInSession(QUESTION_ID, "Explain generics.", "Generics", InterviewDifficulty.INTERMEDIATE);
        SubmitAnswerResponse response =
                new SubmitAnswerResponse(SESSION_ID, ANSWER_ID, InterviewSessionStatus.ACTIVE, nextQuestion);
        when(sessionService.submitAnswer(eq(SESSION_ID), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest("Bytecode runs on the JVM."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(ANSWER_ID.toString()))
                .andExpect(jsonPath("$.sessionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.nextQuestion.questionText").value("Explain generics."));
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitAnswer_lastQuestion_returns200WithNullNextQuestion() throws Exception {
        SubmitAnswerResponse response =
                new SubmitAnswerResponse(SESSION_ID, ANSWER_ID, InterviewSessionStatus.ACTIVE, null);
        when(sessionService.submitAnswer(eq(SESSION_ID), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest("My answer."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextQuestion").doesNotExist());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitAnswer_sessionNotFound_returns404() throws Exception {
        when(sessionService.submitAnswer(eq(SESSION_ID), any(), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest("My answer."))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitAnswer_finishedSession_returns409() throws Exception {
        when(sessionService.submitAnswer(eq(SESSION_ID), any(), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Session already finished"));

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest("My answer."))))
                .andExpect(status().isConflict());
    }

    // ── POST /interview/sessions/{id}/finish ───────────────────────────────────

    @Test
    void finishSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions/{id}/finish", SESSION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void finishSession_validRequest_returns200WithReport() throws Exception {
        EvaluationReport report = stubbedReport(SESSION_ID, 78);
        FinishSessionResponse response =
                new FinishSessionResponse(SESSION_ID, InterviewSessionStatus.FINISHED, 78, report);
        when(sessionService.finishSession(eq(SESSION_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/finish", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.overallScore").value(78))
                .andExpect(jsonPath("$.report.dimensions").isArray())
                .andExpect(jsonPath("$.report.dimensions.length()").value(10));
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void finishSession_alreadyFinished_returns409() throws Exception {
        when(sessionService.finishSession(eq(SESSION_ID), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Session already finished"));

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/finish", SESSION_ID))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void finishSession_sessionNotFound_returns404() throws Exception {
        when(sessionService.finishSession(eq(SESSION_ID), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/finish", SESSION_ID))
                .andExpect(status().isNotFound());
    }

    // ── GET /interview/sessions/{id} ──────────────────────────────────────────

    @Test
    void getSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/interview/sessions/{id}", SESSION_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void getSession_finishedSession_returns200WithReport() throws Exception {
        EvaluationReport report = stubbedReport(SESSION_ID, 88);
        SessionDetailResponse response = new SessionDetailResponse(
                SESSION_ID, "Java", InterviewSessionStatus.FINISHED, 88, report, Instant.parse("2026-07-25T10:00:00Z"));
        when(sessionService.getSession(eq(SESSION_ID), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/interview/sessions/{id}", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.score").value(88))
                .andExpect(jsonPath("$.report.overallScore").value(88))
                .andExpect(jsonPath("$.report.dimensions.length()").value(10));
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void getSession_sessionNotFound_returns404() throws Exception {
        when(sessionService.getSession(eq(SESSION_ID), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        mockMvc.perform(get("/api/v1/interview/sessions/{id}", SESSION_ID)).andExpect(status().isNotFound());
    }

    // ── GET /interview/sessions ────────────────────────────────────────────────

    @Test
    void listSessions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/interview/sessions")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void listSessions_authenticated_returns200WithList() throws Exception {
        UUID sessionId2 = UUID.fromString("66666666-6666-6666-6666-666666666666");
        List<SessionSummaryResponse> summaries = List.of(
                new SessionSummaryResponse(
                        SESSION_ID, "Java", InterviewSessionStatus.FINISHED, 88, Instant.parse("2026-07-25T10:00:00Z")),
                new SessionSummaryResponse(
                        sessionId2,
                        "Spring",
                        InterviewSessionStatus.ACTIVE,
                        null,
                        Instant.parse("2026-07-24T08:00:00Z")));
        when(sessionService.listSessions(any())).thenReturn(summaries);

        mockMvc.perform(get("/api/v1/interview/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("FINISHED"))
                .andExpect(jsonPath("$[0].score").value(88))
                .andExpect(jsonPath("$[1].sessionId").value(sessionId2.toString()))
                .andExpect(jsonPath("$[1].score").doesNotExist());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static EvaluationReport stubbedReport(UUID sessionId, int overallScore) {
        List<EvaluationDimension> dimensions = List.of(
                new EvaluationDimension("technicalAccuracy", overallScore, "Good."),
                new EvaluationDimension("depthOfKnowledge", overallScore, "Good."),
                new EvaluationDimension("practicalApplication", overallScore, "Good."),
                new EvaluationDimension("communicationClarity", overallScore, "Good."),
                new EvaluationDimension("breadthOfCoverage", overallScore, "Good."),
                new EvaluationDimension("exampleQuality", overallScore, "Good."),
                new EvaluationDimension("problemSolvingApproach", overallScore, "Good."),
                new EvaluationDimension("edgeCaseAwareness", overallScore, "Good."),
                new EvaluationDimension("modernJavaAwareness", overallScore, "Good."),
                new EvaluationDimension("learningPotential", overallScore, "Good."));
        return new EvaluationReport(
                sessionId, "Java", overallScore, dimensions, Instant.parse("2026-07-25T10:00:00Z"), null);
    }
}
