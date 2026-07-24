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
     */
    @Transactional
    public void awardTaskCompletion(UUID userId, UUID taskId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        long xpReward = taskRepository.getReferenceById(taskId).getXpReward();
        user.setXpPoints(user.getXpPoints() + xpReward);
        user.setCrystals(user.getCrystals() + 1);
        userRepository.save(user);
    }

    /**
     * Computes the developer level from accumulated XP.
     *
     * <p>Formula: {@code level = min(50, floor(sqrt(xpPoints / 100)) + 1)}. This gives
     * level 1 at 0 XP and level 50 (cap) at ~240 000 XP, with early levels requiring
     * far fewer XP than later ones.
     */
    public static int calculateLevel(long xpPoints) {
        if (xpPoints <= 0) {
            return 1;
        }
        return Math.min(50, (int) Math.sqrt((double) xpPoints / 100) + 1);
    }
}
