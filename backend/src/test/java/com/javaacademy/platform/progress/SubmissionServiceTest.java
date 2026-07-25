package com.javaacademy.platform.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import com.javaacademy.platform.config.PlatformMetrics;
import com.javaacademy.platform.progress.dto.SubmissionResponse;
import com.javaacademy.platform.progress.dto.SubmitResponse;
import com.javaacademy.platform.progress.entity.Submission;
import com.javaacademy.platform.progress.enums.SubmissionStatus;
import com.javaacademy.platform.progress.repository.SubmissionRepository;
import com.javaacademy.platform.progress.service.ProgressService;
import com.javaacademy.platform.progress.service.SubmissionService;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.SubmissionQueue;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SubmissionServiceTest {

    UserRepository userRepository;
    TaskRepository taskRepository;
    SubmissionRepository submissionRepository;
    SubmissionQueue submissionQueue;
    ProgressService progressService;
    CodeExecutionEngine executionEngine;
    PlatformMetrics metrics;
    Clock clock;
    SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        submissionQueue = mock(SubmissionQueue.class);
        progressService = mock(ProgressService.class);
        executionEngine = mock(CodeExecutionEngine.class);
        metrics = mock(PlatformMetrics.class);
        clock = Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC);

        io.micrometer.core.instrument.Timer timer = new SimpleMeterRegistry().timer("test.timer");
        when(metrics.submissionDurationTimer()).thenReturn(timer);

        submissionService = new SubmissionService(
                submissionRepository,
                progressService,
                executionEngine,
                userRepository,
                taskRepository,
                submissionQueue,
                metrics,
                clock);
    }

    // ── createSubmission ───────────────────────────────────────────────────────

    @Test
    void createSubmission_validRequest_savesAndEnqueuesAndReturnsId() {
        String email = "student@test.com";
        UUID taskId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();

        User user = savedUser(email);
        Task task = taskWithId(taskId);
        Submission saved = pendingSubmission(submissionId, user, task);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(submissionRepository.save(any(Submission.class))).thenReturn(saved);

        SubmitResponse response = submissionService.createSubmission(email, taskId, "public class Solution {}");

        assertThat(response.submissionId()).isEqualTo(submissionId);
        verify(submissionQueue).enqueue(submissionId);
    }

    @Test
    void createSubmission_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.createSubmission("nobody@test.com", UUID.randomUUID(), "code"))
                .isInstanceOf(ApiException.class)
                .extracting(throwable -> ((ApiException) throwable).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(submissionQueue, never()).enqueue(any());
    }

    @Test
    void createSubmission_taskNotFound_throwsNotFound() {
        String email = "student@test.com";
        UUID taskId = UUID.randomUUID();
        User user = savedUser(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.createSubmission(email, taskId, "code"))
                .isInstanceOf(ApiException.class)
                .extracting(throwable -> ((ApiException) throwable).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(submissionQueue, never()).enqueue(any());
    }

    // ── findSubmission ─────────────────────────────────────────────────────────

    @Test
    void findSubmission_ownedByUser_returnsResponse() {
        String email = "student@test.com";
        UUID submissionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        User user = savedUser(email);
        Task task = taskWithId(taskId);
        Submission submission = passedSubmission(submissionId, user, task);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(submissionRepository.findByIdAndUser_Id(submissionId, user.getId()))
                .thenReturn(Optional.of(submission));

        SubmissionResponse response = submissionService.findSubmission(submissionId, email);

        assertThat(response.id()).isEqualTo(submissionId);
        assertThat(response.taskId()).isEqualTo(taskId);
        assertThat(response.status()).isEqualTo("PASSED");
        assertThat(response.logs()).isEqualTo("Tests passed");
        assertThat(response.durationMs()).isEqualTo(500L);
    }

    @Test
    void findSubmission_notOwnedByUser_throwsNotFound() {
        String email = "student@test.com";
        UUID submissionId = UUID.randomUUID();
        User user = savedUser(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(submissionRepository.findByIdAndUser_Id(submissionId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.findSubmission(submissionId, email))
                .isInstanceOf(ApiException.class)
                .extracting(throwable -> ((ApiException) throwable).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findSubmission_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.findSubmission(UUID.randomUUID(), "nobody@test.com"))
                .isInstanceOf(ApiException.class)
                .extracting(throwable -> ((ApiException) throwable).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findSubmission_taskDeletedNullTaskRef_returnsNullTaskId() {
        String email = "student@test.com";
        UUID submissionId = UUID.randomUUID();
        User user = savedUser(email);
        Submission submission = pendingSubmission(submissionId, user, null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(submissionRepository.findByIdAndUser_Id(submissionId, user.getId()))
                .thenReturn(Optional.of(submission));

        SubmissionResponse response = submissionService.findSubmission(submissionId, email);

        assertThat(response.taskId()).isNull();
        assertThat(response.status()).isEqualTo("PENDING");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User savedUser(String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn(email);
        return user;
    }

    private Task taskWithId(UUID taskId) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(taskId);
        return task;
    }

    private Submission pendingSubmission(UUID submissionId, User user, Task task) {
        Submission submission = mock(Submission.class);
        when(submission.getId()).thenReturn(submissionId);
        when(submission.getUser()).thenReturn(user);
        when(submission.getTask()).thenReturn(task);
        when(submission.getStatus()).thenReturn(SubmissionStatus.PENDING);
        when(submission.getLogs()).thenReturn(null);
        when(submission.getDurationMs()).thenReturn(null);
        when(submission.getCreatedAt()).thenReturn(Instant.parse("2026-07-24T12:00:00Z"));
        return submission;
    }

    private Submission passedSubmission(UUID submissionId, User user, Task task) {
        Submission submission = mock(Submission.class);
        when(submission.getId()).thenReturn(submissionId);
        when(submission.getUser()).thenReturn(user);
        when(submission.getTask()).thenReturn(task);
        when(submission.getStatus()).thenReturn(SubmissionStatus.PASSED);
        when(submission.getLogs()).thenReturn("Tests passed");
        when(submission.getDurationMs()).thenReturn(500L);
        when(submission.getCreatedAt()).thenReturn(Instant.parse("2026-07-24T12:00:00Z"));
        return submission;
    }
}
