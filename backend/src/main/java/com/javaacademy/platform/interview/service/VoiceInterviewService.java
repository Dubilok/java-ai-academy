package com.javaacademy.platform.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.interview.dto.SubmitVoiceAnswerRequest;
import com.javaacademy.platform.interview.dto.TurnAssessment;
import com.javaacademy.platform.interview.dto.TurnResult;
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import com.javaacademy.platform.interview.entity.InterviewSession;
import com.javaacademy.platform.interview.entity.VoiceInterviewQuestion;
import com.javaacademy.platform.interview.enums.InterviewMode;
import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import com.javaacademy.platform.interview.repository.InterviewAnswerRepository;
import com.javaacademy.platform.interview.repository.InterviewSessionRepository;
import com.javaacademy.platform.interview.repository.VoiceInterviewQuestionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceInterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final VoiceInterviewQuestionRepository voiceQuestionRepository;
    private final UserRepository userRepository;
    private final VoiceInterviewOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    @Transactional
    public TurnResult submitVoiceAnswer(UUID sessionId, SubmitVoiceAnswerRequest request, String userEmail) {
        User user = requireUser(userEmail);

        InterviewSession session = sessionRepository
                .findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));

        if (session.getStatus() == InterviewSessionStatus.FINISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Session " + sessionId + " is already finished");
        }
        if (session.getMode() != InterviewMode.VOICE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Session " + sessionId + " is not a VOICE session");
        }

        @Nullable
        VoiceInterviewQuestion currentVoiceQuestion = voiceQuestionRepository
                .findBySessionIdAndTurnIndex(sessionId, request.turnIndex())
                .orElse(null);

        InterviewAnswer answer = new InterviewAnswer();
        answer.setSession(session);
        answer.setVoiceQuestion(currentVoiceQuestion);
        answer.setTranscript(request.transcript());
        answer.setDynamic(true);
        answerRepository.save(answer);

        int nextTurnIndex = request.turnIndex() + 1;
        TurnResult turnResult = orchestrator.generateNextQuestion(session, request.transcript(), nextTurnIndex);

        if (turnResult.turnAssessment() != null) {
            answer.setTurnAssessmentJson(serializeAssessment(turnResult.turnAssessment()));
        }

        session.setTotalTurns(nextTurnIndex);

        log.info(
                "Voice answer recorded for session {} turn={} isFinalTurn={}",
                sessionId,
                request.turnIndex(),
                turnResult.isFinalTurn());
        return turnResult;
    }

    private String serializeAssessment(TurnAssessment assessment) {
        try {
            return objectMapper.writeValueAsString(assessment);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize turn assessment: " + exception.getMessage());
        }
    }

    private User requireUser(String userEmail) {
        return userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found: " + userEmail));
    }
}
