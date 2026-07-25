package com.javaacademy.platform.ai.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HintService {

    private final SocraticMentorService socraticMentorService;
    private final AiRateLimiter aiRateLimiter;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final AiHintRepository aiHintRepository;
    private final JudgeService judgeService;
    private final Clock clock;

    /**
     * Generates a Socratic hint for the given task and authenticated user (identified by email).
     *
     * <p>Rate-limited to {@link AiRateLimiter#MAX_HINTS_PER_HOUR} hints per user per hour.
     * The last failed submission for the user+task pair is used as context; if none exists,
     * the hint is grounded in the task description alone.
     * Every hint is persisted to {@code ai_hints} for quality auditing.
     */
    @Transactional
    public HintResponse generateHint(UUID taskId, String userEmail) {
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found: " + userEmail));

        UUID userId = user.getId();

        if (!aiRateLimiter.isAllowed(userId)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Hint rate limit reached — you may request up to " + AiRateLimiter.MAX_HINTS_PER_HOUR
                            + " hints per hour");
        }

        Task task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        String errorOutput = submissionRepository
                .findTopByUser_IdAndTask_IdAndStatusOrderByCreatedAtDesc(userId, taskId, SubmissionStatus.FAILED)
                .map(Submission::getLogs)
                .orElse(null);

        HintRequest hintRequest = new HintRequest(task.getTitle(), task.getDescription(), null, errorOutput);

        log.info("Generating Socratic hint for task='{}' user='{}' hasError={}", taskId, userId, errorOutput != null);
        String hint = socraticMentorService.generateHint(hintRequest);

        AiHint aiHint = new AiHint();
        aiHint.setUserId(userId);
        aiHint.setTaskId(taskId);
        aiHint.setHintText(hint);
        aiHint.setCreatedAt(Instant.now(clock));
        AiHint savedHint = aiHintRepository.save(aiHint);

        judgeService.evaluateHintAsync(savedHint.getId(), task.getTitle(), task.getDescription(), hint);

        return new HintResponse(taskId, hint);
    }
}
