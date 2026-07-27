package com.javaacademy.platform.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.interview.dto.EvaluationDimension;
import com.javaacademy.platform.interview.dto.EvaluationReport;
import com.javaacademy.platform.interview.dto.FinishSessionResponse;
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
import com.javaacademy.platform.interview.enums.InterviewSessionStatus;
import com.javaacademy.platform.interview.repository.InterviewAnswerRepository;
import com.javaacademy.platform.interview.repository.InterviewQuestionRepository;
import com.javaacademy.platform.interview.repository.InterviewSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InterviewSessionServiceTest {

    static final UUID USER_ID = UUID.randomUUID();
    static final UUID SESSION_ID = UUID.randomUUID();
    static final UUID QUESTION_ID_1 = UUID.randomUUID();
    static final UUID QUESTION_ID_2 = UUID.randomUUID();
    static final String USER_EMAIL = "student@example.com";
    static final String TECHNOLOGY = "Java";

    InterviewSessionRepository sessionRepository;
    InterviewQuestionRepository questionRepository;
    InterviewAnswerRepository answerRepository;
    UserRepository userRepository;
    MockInterviewerService mockInterviewerService;
    InterviewSessionService service;

    User mockUser;
    InterviewQuestion question1;
    InterviewQuestion question2;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(InterviewSessionRepository.class);
        questionRepository = mock(InterviewQuestionRepository.class);
        answerRepository = mock(InterviewAnswerRepository.class);
        userRepository = mock(UserRepository.class);
        mockInterviewerService = mock(MockInterviewerService.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-25T10:00:00Z"), ZoneOffset.UTC);

        service = new InterviewSessionService(
                sessionRepository,
                questionRepository,
                answerRepository,
                userRepository,
                mockInterviewerService,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                fixedClock);

        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(USER_ID);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));

        question1 = makeQuestion(QUESTION_ID_1, "What is the JVM?", InterviewDifficulty.BEGINNER);
        question2 = makeQuestion(QUESTION_ID_2, "Explain generics.", InterviewDifficulty.INTERMEDIATE);

        when(sessionRepository.save(any())).thenAnswer(invocation -> {
            InterviewSession session = invocation.getArgument(0);
            return setId(session, SESSION_ID);
        });
        when(answerRepository.save(any())).thenAnswer(invocation -> {
            InterviewAnswer answer = invocation.getArgument(0);
            return setAnswerId(answer, UUID.randomUUID());
        });
    }

    // ── startSession ───────────────────────────────────────────────────────────

    @Test
    void startSession_withAvailableQuestions_returnsSessionWithFirstQuestion() {
        when(questionRepository.findByTechnology(TECHNOLOGY)).thenReturn(List.of(question1));

        StartSessionResponse response = service.startSession(new StartSessionRequest(TECHNOLOGY), USER_EMAIL);

        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.technology()).isEqualTo(TECHNOLOGY);
        assertThat(response.status()).isEqualTo(InterviewSessionStatus.ACTIVE);
        assertThat(response.firstQuestion()).isNotNull();
        assertThat(response.firstQuestion().questionText()).isEqualTo("What is the JVM?");
    }

    @Test
    void startSession_persistsSession() {
        when(questionRepository.findByTechnology(TECHNOLOGY)).thenReturn(List.of(question1));

        service.startSession(new StartSessionRequest(TECHNOLOGY), USER_EMAIL);

        verify(sessionRepository).save(any(InterviewSession.class));
    }

    @Test
    void startSession_noQuestionsForTechnology_throws422() {
        when(questionRepository.findByTechnology("Cobol")).thenReturn(List.of());

        assertThatThrownBy(() -> service.startSession(new StartSessionRequest("Cobol"), USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void startSession_unknownUser_throws401() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(new StartSessionRequest(TECHNOLOGY), USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── submitAnswer ───────────────────────────────────────────────────────────

    @Test
    void submitAnswer_savesAnswerAndReturnsNextQuestion() {
        InterviewSession session = activeSession(question1);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findAskedQuestionIdsBySessionId(SESSION_ID)).thenReturn(List.of());
        when(questionRepository.findByTechnologyExcluding(TECHNOLOGY, List.of(QUESTION_ID_1)))
                .thenReturn(List.of(question2));

        SubmitAnswerResponse response =
                service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("The JVM runs bytecode."), USER_EMAIL);

        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.nextQuestion()).isNotNull();
        assertThat(response.nextQuestion().questionId()).isEqualTo(QUESTION_ID_2);
        verify(answerRepository).save(any(InterviewAnswer.class));
    }

    @Test
    void submitAnswer_lastQuestion_returnsNullNextQuestion() {
        InterviewSession session = activeSession(question2);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findAskedQuestionIdsBySessionId(SESSION_ID)).thenReturn(List.of(QUESTION_ID_1));
        when(questionRepository.findByTechnologyExcluding(TECHNOLOGY, List.of(QUESTION_ID_1, QUESTION_ID_2)))
                .thenReturn(List.of());

        SubmitAnswerResponse response = service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("Answer"), USER_EMAIL);

        assertThat(response.nextQuestion()).isNull();
    }

    @Test
    void submitAnswer_sessionNotFound_throws404() {
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("Answer"), USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void submitAnswer_finishedSession_throws409() {
        InterviewSession finishedSession = activeSession(question1);
        finishedSession.setStatus(InterviewSessionStatus.FINISHED);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(finishedSession));

        assertThatThrownBy(() -> service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("Answer"), USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // ── adaptive difficulty ────────────────────────────────────────────────────

    @Test
    void submitAnswer_withHighScores_prefersHigherDifficultyForNextQuestion() {
        // question1 = BEGINNER, question2 = INTERMEDIATE
        // High scores (80, 85) should step up from BEGINNER → INTERMEDIATE
        InterviewSession session = activeSession(question1);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findAskedQuestionIdsBySessionId(SESSION_ID)).thenReturn(List.of());
        when(answerRepository.findScoresBySessionId(SESSION_ID)).thenReturn(List.of(80, 85));
        when(questionRepository.findByTechnologyExcluding(TECHNOLOGY, List.of(QUESTION_ID_1)))
                .thenReturn(List.of(question2));

        SubmitAnswerResponse response =
                service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("The JVM executes bytecode."), USER_EMAIL);

        assertThat(response.nextQuestion()).isNotNull();
        assertThat(response.nextQuestion().difficulty()).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    @Test
    void submitAnswer_withLowScores_prefersLowerDifficultyForNextQuestion() {
        UUID advancedId = UUID.randomUUID();
        InterviewQuestion questionAdvanced =
                makeQuestion(advancedId, "Explain JVM internals.", InterviewDifficulty.ADVANCED);

        // Current question is ADVANCED, low scores (20, 25) → step down to INTERMEDIATE
        InterviewSession session = activeSession(questionAdvanced);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findAskedQuestionIdsBySessionId(SESSION_ID)).thenReturn(List.of());
        when(answerRepository.findScoresBySessionId(SESSION_ID)).thenReturn(List.of(20, 25));
        // Candidates at target difficulty INTERMEDIATE: question2
        when(questionRepository.findByTechnologyExcluding(TECHNOLOGY, List.of(advancedId)))
                .thenReturn(List.of(question1, question2));

        SubmitAnswerResponse response =
                service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("I'm not sure."), USER_EMAIL);

        assertThat(response.nextQuestion()).isNotNull();
        assertThat(response.nextQuestion().difficulty()).isEqualTo(InterviewDifficulty.INTERMEDIATE);
    }

    // ── finishSession ──────────────────────────────────────────────────────────

    @Test
    void finishSession_activeSession_finishesAndReturnsReport() {
        InterviewSession session = activeSession(question1);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findBySessionIdWithQuestionOrderByCreatedAt(SESSION_ID))
                .thenReturn(List.of());

        EvaluationReport stubbedReport = stubbedReport(75);
        when(mockInterviewerService.evaluate(any(), any(), any())).thenReturn(stubbedReport);

        FinishSessionResponse response = service.finishSession(SESSION_ID, USER_EMAIL);

        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.status()).isEqualTo(InterviewSessionStatus.FINISHED);
        assertThat(response.overallScore()).isEqualTo(75);
        assertThat(response.report()).isEqualTo(stubbedReport);
    }

    @Test
    void finishSession_persistsFinishedStatusAndScore() {
        InterviewSession session = activeSession(question1);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findBySessionIdWithQuestionOrderByCreatedAt(SESSION_ID))
                .thenReturn(List.of());
        when(mockInterviewerService.evaluate(any(), any(), any())).thenReturn(stubbedReport(60));

        service.finishSession(SESSION_ID, USER_EMAIL);

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.FINISHED);
        assertThat(session.getScore()).isEqualTo(60);
        assertThat(session.getReportJson()).isNotBlank();
    }

    @Test
    void finishSession_alreadyFinishedSession_throws409() {
        InterviewSession finishedSession = activeSession(question1);
        finishedSession.setStatus(InterviewSessionStatus.FINISHED);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(finishedSession));

        assertThatThrownBy(() -> service.finishSession(SESSION_ID, USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void finishSession_sessionNotFound_throws404() {
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finishSession(SESSION_ID, USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void submitAnswer_withNullScores_retainsCurrentDifficulty() {
        // All scores null (no scoring yet from E6-T4) → stay at current difficulty
        InterviewSession session = activeSession(question2); // current = INTERMEDIATE
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(answerRepository.findAskedQuestionIdsBySessionId(SESSION_ID)).thenReturn(List.of());
        // Mockito returns empty list by default for findScoresBySessionId — explicitly stub null scores
        when(answerRepository.findScoresBySessionId(SESSION_ID)).thenReturn(java.util.Arrays.asList(null, null));
        when(questionRepository.findByTechnologyExcluding(TECHNOLOGY, List.of(QUESTION_ID_2)))
                .thenReturn(List.of(question1, question2));

        // With null scores, target = INTERMEDIATE (unchanged); question2 preferred but excluded
        // so falls back to question1 (BEGINNER) — that's correct fallback behaviour
        SubmitAnswerResponse response = service.submitAnswer(SESSION_ID, new SubmitAnswerRequest("Answer"), USER_EMAIL);

        assertThat(response.nextQuestion()).isNotNull();
    }

    // ── getSession ─────────────────────────────────────────────────────────────

    @Test
    void getSession_activeSession_returnsDetailWithoutReport() {
        InterviewSession session = activeSession(question1);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));

        SessionDetailResponse response = service.getSession(SESSION_ID, USER_EMAIL);

        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.technology()).isEqualTo(TECHNOLOGY);
        assertThat(response.status()).isEqualTo(InterviewSessionStatus.ACTIVE);
        assertThat(response.score()).isNull();
        assertThat(response.report()).isNull();
        assertThat(response.currentQuestion()).isNotNull();
        assertThat(response.currentQuestion().questionText()).isEqualTo(question1.getQuestion());
    }

    @Test
    void getSession_finishedSession_returnsDetailWithDeserializedReport() throws Exception {
        EvaluationReport report = stubbedReport(82);
        String reportJson =
                new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(report);

        InterviewSession session = activeSession(question1);
        session.setStatus(InterviewSessionStatus.FINISHED);
        session.setScore(82);
        session.setReportJson(reportJson);
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));

        SessionDetailResponse response = service.getSession(SESSION_ID, USER_EMAIL);

        assertThat(response.status()).isEqualTo(InterviewSessionStatus.FINISHED);
        assertThat(response.score()).isEqualTo(82);
        assertThat(response.report()).isNotNull();
        assertThat(response.report().overallScore()).isEqualTo(82);
        assertThat(response.report().dimensions()).hasSize(10);
    }

    @Test
    void getSession_sessionNotFound_throws404() {
        when(sessionRepository.findByIdAndUser_Id(SESSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSession(SESSION_ID, USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── listSessions ───────────────────────────────────────────────────────────

    @Test
    void listSessions_withSessions_returnsNewestFirst() {
        InterviewSession older = activeSession(question1);
        older.setCreatedAt(Instant.parse("2026-07-24T08:00:00Z"));
        UUID olderSessionId = UUID.randomUUID();
        setId(older, olderSessionId);

        InterviewSession newer = activeSession(question2);
        newer.setCreatedAt(Instant.parse("2026-07-25T10:00:00Z"));

        when(sessionRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(newer, older));

        List<SessionSummaryResponse> sessions = service.listSessions(USER_EMAIL);

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).sessionId()).isEqualTo(SESSION_ID);
        assertThat(sessions.get(1).sessionId()).isEqualTo(olderSessionId);
    }

    @Test
    void listSessions_noSessions_returnsEmptyList() {
        when(sessionRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

        List<SessionSummaryResponse> sessions = service.listSessions(USER_EMAIL);

        assertThat(sessions).isEmpty();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private EvaluationReport stubbedReport(int overallScore) {
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
                SESSION_ID, TECHNOLOGY, overallScore, dimensions, Instant.parse("2026-07-25T10:00:00Z"), null);
    }

    private InterviewQuestion makeQuestion(UUID id, String questionText, InterviewDifficulty difficulty) {
        InterviewQuestion question = new InterviewQuestion();
        question.setTechnology(TECHNOLOGY);
        question.setCategory("Core");
        question.setQuestion(questionText);
        question.setDifficulty(difficulty);
        try {
            var idField = InterviewQuestion.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(question, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
        return question;
    }

    private InterviewSession activeSession(InterviewQuestion currentQuestion) {
        InterviewSession session = new InterviewSession();
        session.setUser(mockUser);
        session.setTechnology(TECHNOLOGY);
        session.setStatus(InterviewSessionStatus.ACTIVE);
        session.setCurrentQuestion(currentQuestion);
        session.setCreatedAt(Instant.parse("2026-07-25T10:00:00Z"));
        try {
            var idField = InterviewSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, SESSION_ID);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
        return session;
    }

    private static InterviewSession setId(InterviewSession session, UUID id) {
        try {
            var idField = InterviewSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
        return session;
    }

    private static InterviewAnswer setAnswerId(InterviewAnswer answer, UUID id) {
        try {
            var idField = InterviewAnswer.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(answer, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
        return answer;
    }
}
