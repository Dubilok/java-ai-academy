package com.javaacademy.platform.progress.service;

import com.javaacademy.platform.progress.entity.Submission;
import com.javaacademy.platform.progress.enums.SubmissionStatus;
import com.javaacademy.platform.progress.repository.SubmissionRepository;
import com.javaacademy.platform.sandbox.CodeExecutionEngine;
import com.javaacademy.platform.sandbox.dto.ExecutionRequest;
import com.javaacademy.platform.sandbox.dto.ExecutionResult;
import com.javaacademy.platform.sandbox.enums.ExecutionStatus;
import com.javaacademy.platform.sandbox.util.JavaClassNameExtractor;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProgressService progressService;
    private final CodeExecutionEngine executionEngine;

    /**
     * Loads the submission, executes it in the sandbox, persists the verdict, and records progress.
     * The outer transaction keeps the status update atomic with the progress/XP update.
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

        // Release the DB transaction during execution so we don't hold a connection for 5s
        // The entity is detached at this point; we will reattach below.
        submissionRepository.flush();

        ExecutionResult result = executionEngine.execute(request);

        boolean isPassed = result.status() == ExecutionStatus.PASSED;
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
