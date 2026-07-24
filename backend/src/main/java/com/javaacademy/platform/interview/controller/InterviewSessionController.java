package com.javaacademy.platform.interview.controller;

import com.javaacademy.platform.interview.dto.FinishSessionResponse;
import com.javaacademy.platform.interview.dto.StartSessionRequest;
import com.javaacademy.platform.interview.dto.StartSessionResponse;
import com.javaacademy.platform.interview.dto.SubmitAnswerRequest;
import com.javaacademy.platform.interview.dto.SubmitAnswerResponse;
import com.javaacademy.platform.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
public class InterviewSessionController {

    private final InterviewSessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StartSessionResponse startSession(
            @Valid @RequestBody StartSessionRequest request, Authentication authentication) {
        return sessionService.startSession(request, authentication.getName());
    }

    @PostMapping("/{sessionId}/answers")
    public SubmitAnswerResponse submitAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest request,
            Authentication authentication) {
        return sessionService.submitAnswer(sessionId, request, authentication.getName());
    }

    @PostMapping("/{sessionId}/finish")
    public FinishSessionResponse finishSession(@PathVariable UUID sessionId, Authentication authentication) {
        return sessionService.finishSession(sessionId, authentication.getName());
    }
}
