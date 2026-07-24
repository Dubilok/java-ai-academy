package com.javaacademy.platform.sandbox;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionQueue {

    void enqueue(UUID submissionId);

    /**
     * Removes and returns the next submission ID, or empty if the queue remains empty after the
     * given timeout.
     */
    Optional<UUID> dequeue(Duration timeout);
}
