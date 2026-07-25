package com.javaacademy.platform.progress.service;

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
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.SubmissionQueue;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import com.javaacademy.platform.sandbox.util.JavaClassNameExtractor;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProgressService progressService;
    private final CodeExecutionEngine executionEngine;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SubmissionQueue submissionQueue;
    private final PlatformMetrics metrics;
    private final Clock clock;

    @Transactional
    public SubmitResponse createSubmission(String userEmail, UUID taskId, String source) {
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));
        Task task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setTask(task);
        submission.setSource(source);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setCreatedAt(clock.instant());
        Submission saved = submissionRepository.save(submission);

        submissionQueue.enqueue(saved.getId());
        log.debug("Submission {} created for task {} by {}", saved.getId(), taskId, userEmail);
        return new SubmitResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse findSubmission(UUID submissionId, String userEmail) {
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));

        Submission submission = submissionRepository
                .findByIdAndUser_Id(submissionId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found: " + submissionId));

        UUID taskId = submission.getTask() != null ? submission.getTask().getId() : null;
        return new SubmissionResponse(
                submission.getId(),
                taskId,
                submission.getStatus().name(),
                submission.getLogs(),
                submission.getDurationMs(),
                submission.getCreatedAt());
    }

    /**
     * Processes a queued submission: runs it in the sandbox, persists the verdict, records progress.
     * Called by {@link SubmissionWorker} from a virtual thread — never from the request path.
     */
    @Transactional
    public void processSubmission(UUID submissionId) {
        Submission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission not found: " + submissionId));

        var task = submission.getTask();
        if (task == null || task.getTestCode() == null) {
            log.warn("Submission {} has no associated task or test code — marking FAILED", submissionId);
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setLogs("Task or test code is missing");
            return;
        }

        String testClassName = JavaClassNameExtractor.extractPublicClassName(task.getTestCode())
                .orElse("TaskTest");
        ExecutionRequest request =
                new ExecutionRequest(task.getId(), submission.getSource(), task.getTestCode(), testClassName);

        // Flush before the long sandbox call so we don't hold a DB connection for 5s
        submissionRepository.flush();

        long sandboxStartNs = System.nanoTime();
        ExecutionResult result = executionEngine.execute(request);
        metrics.submissionDurationTimer().record(System.nanoTime() - sandboxStartNs, TimeUnit.NANOSECONDS);

        boolean isPassed = result.status() == ExecutionStatus.PASSED;
        if (!isPassed) {
            metrics.recordSandboxFailure();
        }
        submission.setStatus(isPassed ? SubmissionStatus.PASSED : SubmissionStatus.FAILED);
        submission.setLogs(result.logs());
        submission.setDurationMs(result.durationMs());

        if (submission.getUser() != null && task.getId() != null) {
            progressService.recordAttempt(submission.getUser().getId(), task.getId(), submission.getSource(), isPassed);
        }
        log.info(
                "Submission {} finished: status={} failedTests={} durationMs={}",
                submissionId,
                result.status(),
                result.failedTests(),
                result.durationMs());
    }
}
