package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.AiRateLimiter;
import com.javaacademy.platform.ai.dto.HintRequest;
import com.javaacademy.platform.ai.dto.HintResponse;
import com.javaacademy.platform.ai.entity.AiHint;
import com.javaacademy.platform.ai.repository.AiHintRepository;
import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.progress.entity.Submission;
import com.javaacademy.platform.progress.enums.SubmissionStatus;
import com.javaacademy.platform.progress.repository.SubmissionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class HintServiceTest {

    static final UUID TASK_ID = UUID.randomUUID();
    static final UUID USER_ID = UUID.randomUUID();
    static final String USER_EMAIL = "student@example.com";
    static final String HINT_TEXT = "What does the error tell you about the expected value?";

    SocraticMentorService socraticMentorService;
    AiRateLimiter aiRateLimiter;
    UserRepository userRepository;
    TaskRepository taskRepository;
    SubmissionRepository submissionRepository;
    AiHintRepository aiHintRepository;
    HintService service;

    @BeforeEach
    void setUp() {
        socraticMentorService = mock(SocraticMentorService.class);
        aiRateLimiter = mock(AiRateLimiter.class);
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        aiHintRepository = mock(AiHintRepository.class);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC);

        service = new HintService(
                socraticMentorService,
                aiRateLimiter,
                userRepository,
                taskRepository,
                submissionRepository,
                aiHintRepository,
                fixedClock);

        User mockUser = user();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(aiRateLimiter.isAllowed(USER_ID)).thenReturn(true);
        when(submissionRepository.findTopByUser_IdAndTask_IdAndStatusOrderByCreatedAtDesc(
                        USER_ID, TASK_ID, SubmissionStatus.FAILED))
                .thenReturn(Optional.empty());
        when(socraticMentorService.generateHint(any())).thenReturn(HINT_TEXT);
        when(aiHintRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ── happy path ─────────────────────────────────────────────────────────────

    @Test
    void generateHint_happyPath_returnsHintWithTaskId() {
        HintResponse response = service.generateHint(TASK_ID, USER_EMAIL);

        assertThat(response.hint()).isEqualTo(HINT_TEXT);
        assertThat(response.taskId()).isEqualTo(TASK_ID);
    }

    @Test
    void generateHint_happyPath_savesAiHintToRepository() {
        service.generateHint(TASK_ID, USER_EMAIL);

        verify(aiHintRepository).save(any(AiHint.class));
    }

    @Test
    void generateHint_noLastFailedSubmission_callsMentorWithNullError() {
        ArgumentCaptor<HintRequest> captor = ArgumentCaptor.forClass(HintRequest.class);

        service.generateHint(TASK_ID, USER_EMAIL);

        verify(socraticMentorService).generateHint(captor.capture());
        assertThat(captor.getValue().errorOutput()).isNull();
    }

    @Test
    void generateHint_withLastFailedSubmission_passesLogsToMentor() {
        Submission failedSub = mock(Submission.class);
        when(failedSub.getLogs()).thenReturn("AssertionError: expected 5 but was 0");
        when(submissionRepository.findTopByUser_IdAndTask_IdAndStatusOrderByCreatedAtDesc(
                        USER_ID, TASK_ID, SubmissionStatus.FAILED))
                .thenReturn(Optional.of(failedSub));

        ArgumentCaptor<HintRequest> captor = ArgumentCaptor.forClass(HintRequest.class);
        service.generateHint(TASK_ID, USER_EMAIL);

        verify(socraticMentorService).generateHint(captor.capture());
        assertThat(captor.getValue().errorOutput()).isEqualTo("AssertionError: expected 5 but was 0");
    }

    @Test
    void generateHint_taskTitleAndDescription_passedToMentor() {
        ArgumentCaptor<HintRequest> captor = ArgumentCaptor.forClass(HintRequest.class);

        service.generateHint(TASK_ID, USER_EMAIL);

        verify(socraticMentorService).generateHint(captor.capture());
        assertThat(captor.getValue().taskTitle()).isEqualTo("Implement a Point record");
        assertThat(captor.getValue().taskDescription()).contains("Euclidean distance");
    }

    // ── rate limit ─────────────────────────────────────────────────────────────

    @Test
    void generateHint_rateLimitExceeded_throws429() {
        when(aiRateLimiter.isAllowed(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.generateHint(TASK_ID, USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void generateHint_rateLimitExceeded_doesNotCallMentor() {
        when(aiRateLimiter.isAllowed(USER_ID)).thenReturn(false);

        try {
            service.generateHint(TASK_ID, USER_EMAIL);
        } catch (ApiException ignored) {
        }

        verify(socraticMentorService, never()).generateHint(any());
    }

    // ── not found ──────────────────────────────────────────────────────────────

    @Test
    void generateHint_taskNotFound_throws404() {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateHint(TASK_ID, USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generateHint_userNotFound_throws401() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateHint(TASK_ID, USER_EMAIL))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        return user;
    }

    private static Task task() {
        Task task = new Task();
        task.setTitle("Implement a Point record");
        task.setDescription("Create a record that computes the Euclidean distance.");
        return task;
    }
}
