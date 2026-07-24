package com.javaacademy.platform.progress.service;

import com.javaacademy.platform.auth.entity.User;
import com.javaacademy.platform.auth.repository.UserRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    /**
     * Awards XP and one crystal to the user for completing a task for the first time.
     * Callers must ensure this is invoked only on the first pass — the idempotency guard
     * lives in ProgressService.recordAttempt (status transition IN_PROGRESS → PASSED).
     * No explicit save needed: Hibernate dirty-checking flushes the mutation at commit.
     */
    @Transactional
    public void awardTaskCompletion(UUID userId, UUID taskId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        long xpReward = taskRepository.getReferenceById(taskId).getXpReward();
        user.setXpPoints(user.getXpPoints() + xpReward);
        user.setCrystals(user.getCrystals() + 1);
    }
}
