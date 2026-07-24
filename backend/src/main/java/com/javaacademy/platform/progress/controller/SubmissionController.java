package com.javaacademy.platform.progress.controller;

import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.progress.dto.SubmissionResponse;
import com.javaacademy.platform.progress.dto.SubmitCodeRequest;
import com.javaacademy.platform.progress.dto.SubmitResponse;
import com.javaacademy.platform.progress.service.SubmissionService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

    static final long SSE_TIMEOUT_MS = 30_000L;
    static final long SSE_POLL_INTERVAL_MS = 500L;

    private final SubmissionService submissionService;

    @PostMapping("/tasks/{taskId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmitResponse submitCode(
            @PathVariable UUID taskId, @Valid @RequestBody SubmitCodeRequest request, Authentication authentication) {
        return submissionService.createSubmission(authentication.getName(), taskId, request.source());
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionResponse getSubmission(@PathVariable UUID submissionId, Authentication authentication) {
        return submissionService.findSubmission(submissionId, authentication.getName());
    }

    @GetMapping(value = "/submissions/{submissionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSubmission(@PathVariable UUID submissionId, Authentication authentication) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String userEmail = authentication.getName();
        Thread.ofVirtual().start(() -> pollUntilTerminal(emitter, submissionId, userEmail));
        return emitter;
    }

    private void pollUntilTerminal(SseEmitter emitter, UUID submissionId, String userEmail) {
        try {
            while (true) {
                SubmissionResponse response = submissionService.findSubmission(submissionId, userEmail);
                emitter.send(SseEmitter.event().name("verdict").data(response, MediaType.APPLICATION_JSON));
                if (isTerminal(response.status())) {
                    emitter.complete();
                    return;
                }
                Thread.sleep(SSE_POLL_INTERVAL_MS);
            }
        } catch (IOException clientDisconnected) {
            // Client closed connection before completion — normal SSE teardown
        } catch (IllegalStateException emitterAlreadyComplete) {
            // Emitter timed out or was completed externally — stop polling
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } catch (ApiException apiException) {
            log.warn("SSE stream error for submission {}: {}", submissionId, apiException.getMessage());
            sendErrorEventAndComplete(emitter, apiException.getMessage());
        }
    }

    private static void sendErrorEventAndComplete(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event().name("error").data(errorMessage, MediaType.TEXT_PLAIN));
        } catch (IOException | IllegalStateException ignored) {
            // Client disconnected or emitter already closed — nothing to send
        }
        emitter.complete();
    }

    private static boolean isTerminal(String status) {
        return "PASSED".equals(status) || "FAILED".equals(status);
    }
}
