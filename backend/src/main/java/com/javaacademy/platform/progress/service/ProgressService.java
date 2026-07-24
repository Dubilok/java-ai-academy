package com.javaacademy.platform.progress.service;

import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.progress.entity.UserProgress;
import com.javaacademy.platform.progress.enums.ProgressStatus;
import com.javaacademy.platform.progress.repository.UserProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    /**
     * Upserts user_progress after a submission verdict and increments the attempt counter.
     *
     * <p>Status transitions:
     *
     * <ul>
     *   <li>No row: create IN_PROGRESS; if passed, create PASSED immediately.
     *   <li>IN_PROGRESS + failed: stay IN_PROGRESS.
     *   <li>IN_PROGRESS + passed: transition to PASSED.
     *   <li>PASSED + any verdict: stay PASSED (idempotent; XP was already awarded).
     * </ul>
     *
     * @return true only when the student passes for the first time — caller should then award XP
     */
    @Transactional
    public boolean recordAttempt(UUID userId, UUID taskId, @Nullable String submittedCode, boolean passed) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndTaskId(userId, taskId)
                .orElseGet(() -> newProgress(userId, taskId));

        boolean wasAlreadyPassed = progress.getStatus() == ProgressStatus.PASSED;

        progress.setAttempts(progress.getAttempts() + 1);
        progress.setSubmittedCode(submittedCode);
        progress.setUpdatedAt(clock.instant());

        if (passed && !wasAlreadyPassed) {
            progress.setStatus(ProgressStatus.PASSED);
        } else if (!passed && !wasAlreadyPassed) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }
        // wasAlreadyPassed: keep PASSED regardless — no further transition

        userProgressRepository.save(progress);

        boolean firstPass = passed && !wasAlreadyPassed;
        log.debug(
                "Recorded attempt userId={} taskId={} passed={} attempts={} firstPass={}",
                userId,
                taskId,
                passed,
                progress.getAttempts(),
                firstPass);
        return firstPass;
    }

    @Transactional(readOnly = true)
    public Optional<UserProgress> findProgress(UUID userId, UUID taskId) {
        return userProgressRepository.findByUserIdAndTaskId(userId, taskId);
    }

    private UserProgress newProgress(UUID userId, UUID taskId) {
        UserProgress progress = new UserProgress();
        // getReferenceById returns a JPA proxy: sets the FK column without a SELECT
        progress.setUser(userRepository.getReferenceById(userId));
        progress.setTask(taskRepository.getReferenceById(taskId));
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setAttempts(0);
        progress.setUpdatedAt(Instant.EPOCH); // overwritten immediately by caller
        return progress;
    }
}
