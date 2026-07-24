package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.catalog.service.ContentImportService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationJobService {

    private final ContentArchitectService contentArchitectService;
    private final ContentImportService contentImportService;

    private final ConcurrentHashMap<UUID, JobState> jobs = new ConcurrentHashMap<>();

    /**
     * Starts an async AI course-generation job and returns its ID immediately.
     * The caller should poll {@link #getJob(UUID)} to track progress.
     */
    public UUID startJob(String technology) {
        UUID jobId = UUID.randomUUID();
        JobState state = new JobState(jobId, technology);
        jobs.put(jobId, state);

        Thread.ofVirtual().name("gen-job-" + jobId).start(() -> runJob(state));

        log.info("Started generation job {} for '{}'", jobId, technology);
        return jobId;
    }

    public Optional<GenerationJobResponse> getJob(UUID jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(state.toResponse());
    }

    private void runJob(JobState state) {
        try {
            var content = contentArchitectService.generateForTopic(state.technology);
            UUID courseId = contentImportService.importGenerated(state.technology, content);
            state.succeed(courseId);
            log.info("Generation job {} succeeded, courseId={}", state.jobId, courseId);
        } catch (Exception exception) {
            state.fail(exception.getMessage());
            log.error("Generation job {} failed: {}", state.jobId, exception.getMessage());
        }
    }

    static final class JobState {

        final UUID jobId;
        final String technology;
        volatile JobStatus status = JobStatus.RUNNING;
        volatile UUID courseId;
        volatile String errorMessage;

        JobState(UUID jobId, String technology) {
            this.jobId = jobId;
            this.technology = technology;
        }

        void succeed(UUID generatedCourseId) {
            this.courseId = generatedCourseId;
            this.status = JobStatus.SUCCEEDED;
        }

        void fail(String message) {
            this.errorMessage = message;
            this.status = JobStatus.FAILED;
        }

        GenerationJobResponse toResponse() {
            return new GenerationJobResponse(jobId, technology, status, courseId, errorMessage);
        }
    }
}
