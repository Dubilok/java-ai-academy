package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.dto.GenerateCourseRequest;
import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.catalog.service.ContentImportService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationJobService {

    private final ContentArchitectService contentArchitectService;
    private final ContentImportService contentImportService;

    private final ConcurrentHashMap<UUID, JobState> jobs = new ConcurrentHashMap<>();

    public UUID startJob(GenerateCourseRequest request) {
        UUID jobId = UUID.randomUUID();
        JobState state = new JobState(jobId, request.technology(), request.courseId(), request.moduleId(), request.moduleName());
        jobs.put(jobId, state);

        Thread.ofVirtual().name("gen-job-" + jobId).start(() -> runJob(state));

        log.info("Started generation job {} for '{}' (courseId={}, moduleId={})",
                jobId, request.technology(), request.courseId(), request.moduleId());
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
            UUID courseId = importContent(state, content);
            state.succeed(courseId);
            log.info("Generation job {} succeeded, courseId={}", state.jobId, courseId);
        } catch (Exception exception) {
            state.fail(exception.getMessage());
            log.error("Generation job {} failed: {}", state.jobId, exception.getMessage());
        }
    }

    private UUID importContent(JobState state, com.javaacademy.platform.ai.dto.GeneratedContent content) {
        if (state.moduleId != null) {
            return contentImportService.importIntoModule(state.moduleId, content);
        }
        if (state.courseId != null) {
            String moduleName = state.moduleName != null ? state.moduleName : state.technology;
            return contentImportService.importIntoNewModule(state.courseId, moduleName, content);
        }
        return contentImportService.importGenerated(state.technology, content);
    }

    static final class JobState {

        final UUID jobId;
        final String technology;
        @Nullable final UUID courseId;
        @Nullable final UUID moduleId;
        @Nullable final String moduleName;
        volatile JobStatus status = JobStatus.RUNNING;
        volatile UUID resultCourseId;
        volatile String errorMessage;

        JobState(UUID jobId, String technology, @Nullable UUID courseId, @Nullable UUID moduleId, @Nullable String moduleName) {
            this.jobId = jobId;
            this.technology = technology;
            this.courseId = courseId;
            this.moduleId = moduleId;
            this.moduleName = moduleName;
        }

        void succeed(UUID generatedCourseId) {
            this.resultCourseId = generatedCourseId;
            this.status = JobStatus.SUCCEEDED;
        }

        void fail(String message) {
            this.errorMessage = message;
            this.status = JobStatus.FAILED;
        }

        GenerationJobResponse toResponse() {
            return new GenerationJobResponse(jobId, technology, status, resultCourseId, errorMessage);
        }
    }
}
