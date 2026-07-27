package com.javaacademy.platform.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import com.javaacademy.platform.interview.dto.FinishSessionResponse;
import com.javaacademy.platform.interview.dto.QuestionInSession;
import com.javaacademy.platform.interview.dto.SessionDetailResponse;
import com.javaacademy.platform.interview.dto.SessionSummaryResponse;
import com.javaacademy.platform.interview.dto.StartSessionRequest;
import com.javaacademy.platform.interview.dto.StartSessionResponse;
import com.javaacademy.platform.interview.dto.SubmitAnswerRequest;
import com.javaacademy.platform.interview.dto.SubmitAnswerResponse;
import com.javaacademy.platform.interview.entity.InterviewAnswer;
import com.javaacademy.platform.interview.entity.InterviewQuestion;
import com.javaacademy.platform.interview.entity.InterviewSession;
import com.javaacademy.platform.interview.enums.InterviewDifficulty;
import com.javaacademy.platform.interview.enums.InterviewMode;
import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import com.javaacademy.platform.interview.repository.InterviewAnswerRepository;
import com.javaacademy.platform.interview.repository.InterviewQuestionRepository;
import com.javaacademy.platform.interview.repository.InterviewSessionRepository;
import com.javaacademy.platform.interview.util.AdaptiveDifficultySelector;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final MockInterviewerService mockInterviewerService;
    private final VoiceInterviewOrchestrator voiceOrchestrator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public StartSessionResponse startSession(StartSessionRequest request, String userEmail) {
        User user = requireUser(userEmail);
        InterviewMode mode = request.mode() != null ? request.mode() : InterviewMode.TEXT;

        if (mode == InterviewMode.VOICE) {
            return startVoiceSession(request, user, mode);
        }
        return startTextSession(request, user, mode);
    }

    private StartSessionResponse startTextSession(StartSessionRequest request, User user, InterviewMode mode) {
        List<InterviewQuestion> available = questionRepository.findByTechnology(request.technology());
        if (available.isEmpty()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No interview questions found for technology: " + request.technology());
        }

        InterviewQuestion firstQuestion = pickRandom(available);

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setTechnology(request.technology());
        session.setStatus(InterviewSessionStatus.ACTIVE);
        session.setMode(mode);
        if (request.maxTurns() != null) {
            session.setMaxTurns(request.maxTurns());
        }
        session.setCurrentQuestion(firstQuestion);
        session.setCreatedAt(Instant.now(clock));
        InterviewSession saved = sessionRepository.save(session);

        log.info(
                "Started TEXT interview session {} for user {} technology={}",
                saved.getId(),
                user.getId(),
                request.technology());
        return new StartSessionResponse(
                saved.getId(),
                saved.getTechnology(),
                saved.getStatus(),
                mode,
                toQuestionInSession(firstQuestion),
                null);
    }

    private StartSessionResponse startVoiceSession(StartSessionRequest request, User user, InterviewMode mode) {
        int maxTurns = request.maxTurns() != null ? request.maxTurns() : 5;

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setTechnology(request.technology());
        session.setStatus(InterviewSessionStatus.ACTIVE);
        session.setMode(mode);
        session.setMaxTurns(maxTurns);
        session.setCreatedAt(Instant.now(clock));
        InterviewSession saved = sessionRepository.save(session);

        com.javaacademy.platform.interview.dto.TurnResult openingTurn =
                voiceOrchestrator.generateOpeningQuestion(saved, maxTurns);

        log.info(
                "Started VOICE interview session {} for user {} technology={} maxTurns={}",
                saved.getId(),
                user.getId(),
                request.technology(),
                maxTurns);
        return new StartSessionResponse(
                saved.getId(), saved.getTechnology(), saved.getStatus(), mode, null, openingTurn.question());
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(UUID sessionId, SubmitAnswerRequest request, String userEmail) {
        User user = requireUser(userEmail);

        InterviewSession session = sessionRepository
                .findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));

        if (session.getStatus() == InterviewSessionStatus.FINISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Session " + sessionId + " is already finished");
        }

        InterviewQuestion currentQuestion = session.getCurrentQuestion();
        if (currentQuestion == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Session " + sessionId + " has no current question");
        }

        InterviewAnswer answer = new InterviewAnswer();
        answer.setSession(session);
        answer.setQuestion(currentQuestion);
        answer.setAnswerText(request.answerText());
        InterviewAnswer savedAnswer = answerRepository.save(answer);

        @Nullable InterviewQuestion nextQuestion = pickNextQuestion(session, currentQuestion.getId());
        session.setCurrentQuestion(nextQuestion);

        return new SubmitAnswerResponse(
                sessionId,
                savedAnswer.getId(),
                session.getStatus(),
                nextQuestion != null ? toQuestionInSession(nextQuestion) : null);
    }

    private @Nullable InterviewQuestion pickNextQuestion(InterviewSession session, UUID justAnsweredId) {
        List<UUID> askedIds = answerRepository.findAskedQuestionIdsBySessionId(session.getId());
        List<UUID> excludedIds = new ArrayList<>(askedIds);
        if (!excludedIds.contains(justAnsweredId)) {
            excludedIds.add(justAnsweredId);
        }

        List<InterviewQuestion> candidates =
                questionRepository.findByTechnologyExcluding(session.getTechnology(), excludedIds);
        if (candidates.isEmpty()) {
            return null;
        }

        InterviewDifficulty currentDifficulty = session.getCurrentQuestion() != null
                ? session.getCurrentQuestion().getDifficulty()
                : InterviewDifficulty.INTERMEDIATE;
        List<Integer> recentScores = answerRepository.findScoresBySessionId(session.getId());
        InterviewDifficulty targetDifficulty = AdaptiveDifficultySelector.selectNext(currentDifficulty, recentScores);

        List<InterviewQuestion> preferred = candidates.stream()
                .filter(question -> question.getDifficulty() == targetDifficulty)
                .toList();

        return pickRandom(preferred.isEmpty() ? candidates : preferred);
    }

    @Transactional
    public FinishSessionResponse finishSession(UUID sessionId, String userEmail) {
        User user = requireUser(userEmail);

        InterviewSession session = sessionRepository
                .findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));

        if (session.getStatus() == InterviewSessionStatus.FINISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Session " + sessionId + " is already finished");
        }

        List<InterviewAnswer> answers = session.getMode() == InterviewMode.VOICE
                ? answerRepository.findBySessionIdWithVoiceQuestionOrderByCreatedAt(sessionId)
                : answerRepository.findBySessionIdWithQuestionOrderByCreatedAt(sessionId);

        EvaluationReport report = mockInterviewerService.evaluate(answers, sessionId, session.getTechnology());

        session.setStatus(InterviewSessionStatus.FINISHED);
        session.setScore(report.overallScore());
        session.setReportJson(serializeReport(report));

        log.info(
                "Finished interview session {} for user {} overall-score={}",
                sessionId,
                user.getId(),
                report.overallScore());
        return new FinishSessionResponse(sessionId, InterviewSessionStatus.FINISHED, report.overallScore(), report);
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse getSession(UUID sessionId, String userEmail) {
        User user = requireUser(userEmail);
        InterviewSession session = sessionRepository
                .findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
        return toDetailResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> listSessions(String userEmail) {
        User user = requireUser(userEmail);
        return sessionRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(InterviewSessionService::toSummaryResponse)
                .toList();
    }

    private SessionDetailResponse toDetailResponse(InterviewSession session) {
        EvaluationReport report = null;
        if (session.getReportJson() != null) {
            report = deserializeReport(session.getReportJson());
        }
        QuestionInSession currentQuestion =
                session.getCurrentQuestion() != null ? toQuestionInSession(session.getCurrentQuestion()) : null;
        return new SessionDetailResponse(
                session.getId(),
                session.getTechnology(),
                session.getStatus(),
                session.getScore(),
                report,
                currentQuestion,
                session.getCreatedAt());
    }

    private static SessionSummaryResponse toSummaryResponse(InterviewSession session) {
        return new SessionSummaryResponse(
                session.getId(),
                session.getTechnology(),
                session.getStatus(),
                session.getScore(),
                session.getCreatedAt());
    }

    private EvaluationReport deserializeReport(String reportJson) {
        try {
            return objectMapper.readValue(reportJson, EvaluationReport.class);
        } catch (JsonProcessingException deserializationException) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to deserialize session report: " + deserializationException.getMessage());
        }
    }

    private String serializeReport(EvaluationReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException serializationException) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize evaluation report: " + serializationException.getMessage());
        }
    }

    private User requireUser(String userEmail) {
        return userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found: " + userEmail));
    }

    private static <T> T pickRandom(List<T> items) {
        List<T> shuffled = new ArrayList<>(items);
        Collections.shuffle(shuffled);
        return shuffled.getFirst();
    }

    private static QuestionInSession toQuestionInSession(InterviewQuestion question) {
        return new QuestionInSession(
                question.getId(), question.getQuestion(), question.getCategory(), question.getDifficulty());
    }
}
