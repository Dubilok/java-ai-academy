package com.javaacademy.platform.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.progress.entity.UserProgress;
import com.javaacademy.platform.progress.repository.UserProgressRepository;
import com.javaacademy.platform.progress.service.ProgressService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    UserProgressRepository userProgressRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    TaskRepository taskRepository;

    @Mock
    Clock clock;

    @InjectMocks
    ProgressService service;

    static final UUID USER_ID = UUID.randomUUID();
    static final UUID TASK_ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-07-24T10:00:00Z");
    static final String CODE = "System.out.println(\"hi\");";

    @BeforeEach
    void freezeClock() {
        lenient().when(clock.instant()).thenReturn(NOW);
    }

    // ── no existing row ───────────────────────────────────────────────────────

    @Test
    void recordAttempt_noRow_failed_createsInProgressWithOneAttempt() {
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(taskRepository.getReferenceById(TASK_ID)).thenReturn(new Task());
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean firstPass = service.recordAttempt(USER_ID, TASK_ID, CODE, false);

        assertThat(firstPass).isFalse();
        ArgumentCaptor<UserProgress> cap = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(cap.capture());
        UserProgress saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(ProgressService.STATUS_IN_PROGRESS);
        assertThat(saved.getAttempts()).isEqualTo(1);
        assertThat(saved.getSubmittedCode()).isEqualTo(CODE);
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void recordAttempt_noRow_passed_createsPassedAndReturnsTrue() {
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(taskRepository.getReferenceById(TASK_ID)).thenReturn(new Task());
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean firstPass = service.recordAttempt(USER_ID, TASK_ID, CODE, true);

        assertThat(firstPass).isTrue();
        ArgumentCaptor<UserProgress> cap = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ProgressService.STATUS_PASSED);
        assertThat(cap.getValue().getAttempts()).isEqualTo(1);
    }

    // ── existing IN_PROGRESS row ──────────────────────────────────────────────

    @Test
    void recordAttempt_inProgress_failed_remainsInProgressIncrementsAttempts() {
        UserProgress existing = inProgressAt(3);
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.of(existing));
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean firstPass = service.recordAttempt(USER_ID, TASK_ID, CODE, false);

        assertThat(firstPass).isFalse();
        assertThat(existing.getStatus()).isEqualTo(ProgressService.STATUS_IN_PROGRESS);
        assertThat(existing.getAttempts()).isEqualTo(4);
    }

    @Test
    void recordAttempt_inProgress_passed_transitionsToPassedReturnsTrue() {
        UserProgress existing = inProgressAt(4);
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.of(existing));
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean firstPass = service.recordAttempt(USER_ID, TASK_ID, CODE, true);

        assertThat(firstPass).isTrue();
        assertThat(existing.getStatus()).isEqualTo(ProgressService.STATUS_PASSED);
        assertThat(existing.getAttempts()).isEqualTo(5);
    }

    // ── already PASSED (idempotent guard) ────────────────────────────────────

    @Test
    void recordAttempt_alreadyPassed_failed_staysPassedReturnsFalse() {
        UserProgress existing = passedAt(1);
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.of(existing));
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean firstPass = service.recordAttempt(USER_ID, TASK_ID, CODE, false);

        assertThat(firstPass).isFalse();
        assertThat(existing.getStatus()).isEqualTo(ProgressService.STATUS_PASSED);
        assertThat(existing.getAttempts()).isEqualTo(2);
    }

    @Test
    void recordAttempt_alreadyPassed_passedAgain_staysPassedReturnsFalse() {
        UserProgress existing = passedAt(2);
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.of(existing));
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean firstPass = service.recordAttempt(USER_ID, TASK_ID, CODE, true);

        assertThat(firstPass).isFalse();
        assertThat(existing.getStatus()).isEqualTo(ProgressService.STATUS_PASSED);
        assertThat(existing.getAttempts()).isEqualTo(3);
    }

    // ── side-effect checks ────────────────────────────────────────────────────

    @Test
    void recordAttempt_always_updatesSubmittedCodeAndTimestamp() {
        UserProgress existing = inProgressAt(1);
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.of(existing));
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordAttempt(USER_ID, TASK_ID, "new code", false);

        assertThat(existing.getSubmittedCode()).isEqualTo("new code");
        assertThat(existing.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void recordAttempt_nullSubmittedCode_accepted() {
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(taskRepository.getReferenceById(TASK_ID)).thenReturn(new Task());
        when(userProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordAttempt(USER_ID, TASK_ID, null, false);

        ArgumentCaptor<UserProgress> cap = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(cap.capture());
        assertThat(cap.getValue().getSubmittedCode()).isNull();
    }

    // ── findProgress ──────────────────────────────────────────────────────────

    @Test
    void findProgress_delegatesToRepository() {
        UserProgress existing = passedAt(3);
        when(userProgressRepository.findByUserIdAndTaskId(USER_ID, TASK_ID)).thenReturn(Optional.of(existing));

        Optional<UserProgress> result = service.findProgress(USER_ID, TASK_ID);

        assertThat(result).contains(existing);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserProgress inProgressAt(int attempts) {
        UserProgress p = new UserProgress();
        p.setStatus(ProgressService.STATUS_IN_PROGRESS);
        p.setAttempts(attempts);
        p.setUpdatedAt(Instant.EPOCH);
        return p;
    }

    private UserProgress passedAt(int attempts) {
        UserProgress p = new UserProgress();
        p.setStatus(ProgressService.STATUS_PASSED);
        p.setAttempts(attempts);
        p.setUpdatedAt(Instant.EPOCH);
        return p;
    }
}
