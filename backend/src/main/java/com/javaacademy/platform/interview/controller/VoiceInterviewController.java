package com.javaacademy.platform.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.interview.dto.SubmitVoiceAnswerRequest;
import com.javaacademy.platform.interview.dto.TurnResult;
import com.javaacademy.platform.interview.service.VoiceInterviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/interview/sessions")
@RequiredArgsConstructor
public class VoiceInterviewController {

    static final long SSE_TIMEOUT_MS = 30_000L;

    private final VoiceInterviewService voiceInterviewService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{sessionId}/voice-answers")
    public SseEmitter submitVoiceAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitVoiceAnswerRequest request,
            Authentication authentication) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        Thread.ofVirtual().start(() -> {
            try {
                TurnResult result =
                        voiceInterviewService.submitVoiceAnswer(sessionId, request, authentication.getName());

                String question = result.question();
                if (question != null && !question.isBlank()) {
                    String[] words = question.split("(?<=\\s)|(?=\\s)");
                    for (String word : words) {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(objectMapper.writeValueAsString(new TokenPayload(word))));
                    }
                }

                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(result)));
                emitter.complete();
            } catch (Exception exception) {
                log.error("Error streaming voice answer for session {}", sessionId, exception);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(new ErrorPayload(exception.getMessage()))));
                } catch (Exception sendException) {
                    log.error("Failed to send SSE error event", sendException);
                }
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }

    record TokenPayload(String value) {}

    record ErrorPayload(String message) {}
}
