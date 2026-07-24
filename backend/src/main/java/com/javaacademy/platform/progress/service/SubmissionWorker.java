package com.javaacademy.platform.progress.service;

import com.javaacademy.platform.sandbox.SubmissionQueue;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionWorker {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);

    private final SubmissionQueue queue;
    private final SubmissionService submissionService;

    @PostConstruct
    void startWorker() {
        Thread.ofVirtual().name("submission-worker").start(this::workerLoop);
    }

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                queue.dequeue(POLL_TIMEOUT).ifPresent(submissionId -> {
                    log.debug("Dequeued submission {}", submissionId);
                    submissionService.processSubmission(submissionId);
                });
            } catch (Exception workerException) {
                log.error("Submission worker error — continuing", workerException);
            }
        }
        log.info("Submission worker thread exiting");
    }
}
