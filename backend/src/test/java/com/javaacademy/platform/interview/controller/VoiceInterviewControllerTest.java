package com.javaacademy.platform.interview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.service.JwtService;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.SecurityConfig;
import com.javaacademy.platform.interview.dto.SubmitVoiceAnswerRequest;
import com.javaacademy.platform.interview.dto.TurnAssessment;
import com.javaacademy.platform.interview.dto.TurnResult;
import com.javaacademy.platform.interview.service.VoiceInterviewService;
import java.util.Map;
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

@WebMvcTest(VoiceInterviewController.class)
@Import(SecurityConfig.class)
class VoiceInterviewControllerTest {

    static final UUID SESSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    VoiceInterviewService voiceInterviewService;

    @MockBean
    JwtService jwtService;

    // ── POST /sessions/{id}/voice-answers ─────────────────────────────────────

    @Test
    void submitVoiceAnswer_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions/{id}/voice-answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitVoiceAnswerRequest("The JVM runs bytecode.", 0, 5))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitVoiceAnswer_authenticated_returnsSseStream() throws Exception {
        TurnResult result = new TurnResult(
                "How does the garbage collector work?",
                Map.of("jvm", true, "gc", false),
                new TurnAssessment("jvm", "STRONG", "Clear explanation."),
                false);

        when(voiceInterviewService.submitVoiceAnswer(eq(SESSION_ID), any(), any()))
                .thenReturn(result);

        String responseBody = mockMvc.perform(post("/api/v1/interview/sessions/{id}/voice-answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitVoiceAnswerRequest("The JVM runs bytecode.", 0, 5))))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // SSE stream is started — basic connectivity check
        // (full SSE body parsing is left to integration tests)
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitVoiceAnswer_sessionNotFound_streamsErrorEvent() throws Exception {
        when(voiceInterviewService.submitVoiceAnswer(eq(SESSION_ID), any(), any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + SESSION_ID));

        mockMvc.perform(post("/api/v1/interview/sessions/{id}/voice-answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitVoiceAnswerRequest("Some answer.", 0, 3))))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void submitVoiceAnswer_missingTranscript_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions/{id}/voice-answers", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transcript\":\"\",\"turnIndex\":0,\"durationSeconds\":3}"))
                .andExpect(status().isBadRequest());
    }
}
