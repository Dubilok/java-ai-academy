package com.javaacademy.platform.ai.service;

import com.javaacademy.platform.ai.dto.ConfirmedCurriculum;
import com.javaacademy.platform.ai.dto.ConfirmedLecture;
import com.javaacademy.platform.ai.dto.ConfirmedModule;
import com.javaacademy.platform.ai.dto.GenerateCourseRequest;
import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.catalog.service.ContentImportService;
import com.javaacademy.platform.catalog.service.ContentImportService.ImportResult;
import java.util.Comparator;
import java.util.List;
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
        JobState state = new JobState(
                jobId,
                request.technology(),
                request.courseId(),
                request.moduleId(),
                request.moduleName(),
                request.curriculum());
        jobs.put(jobId, state);

        Thread.ofVirtual().name("gen-job-" + jobId).start(() -> runJob(state));

        log.info(
                "Started generation job {} for '{}' (mode={})",
                jobId,
                request.technology(),
                request.curriculum() != null ? "curriculum" : "single");
        return jobId;
    }

    public Optional<GenerationJobResponse> getJob(UUID jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(state.toResponse());
    }

    public List<GenerationJobResponse> listJobs() {
        return jobs.values().stream()
                .map(JobState::toResponse)
                .sorted(Comparator.comparing(GenerationJobResponse::status,
                        Comparator.comparingInt(s -> switch (s) {
                            case RUNNING -> 0;
                            case FAILED -> 1;
                            case SUCCEEDED -> 2;
                        }))
                        .thenComparing(Comparator.comparingInt(GenerationJobResponse::completedItems).reversed()))
                .toList();
    }

    // ── Job runners ───────────────────────────────────────────────────────────

    private void runJob(JobState state) {
        try {
            UUID courseId = state.curriculum != null ? runCurriculumJob(state) : runSingleJob(state);
            state.succeed(courseId);
            log.info("Generation job {} succeeded, courseId={}", state.jobId, courseId);
        } catch (Exception exception) {
            state.fail(exception.getMessage());
            log.error("Generation job {} failed: {}", state.jobId, exception.getMessage());
        }
    }

    /** Legacy single-lecture mode. */
    private UUID runSingleJob(JobState state) {
        var content = contentArchitectService.generateForTopic(state.technology);
        if (state.moduleId != null) {
            return contentImportService.importIntoModule(state.moduleId, content);
        }
        if (state.courseId != null) {
            String moduleName = state.moduleName != null ? state.moduleName : state.technology;
            return contentImportService.importIntoNewModule(state.courseId, moduleName, content);
        }
        return contentImportService.importGenerated(state.technology, content);
    }

    /** Full curriculum mode — generates multiple modules, lectures, and tasks. */
    private UUID runCurriculumJob(JobState state) {
        ConfirmedCurriculum curriculum = state.curriculum;

        // Count total tasks up front so the UI can show a progress bar.
        int totalTasks = 0;
        for (ConfirmedModule confirmedModule : curriculum.modules()) {
            for (ConfirmedLecture confirmedLecture : confirmedModule.lectures()) {
                totalTasks += confirmedLecture.taskCount();
            }
        }
        state.totalItems = totalTasks;

        UUID courseId = contentImportService.createCourse(
                state.technology, curriculum.courseName(), curriculum.description());

        List<ConfirmedModule> modules = curriculum.modules();
        for (int moduleIdx = 0; moduleIdx < modules.size(); moduleIdx++) {
            ConfirmedModule confirmedModule = modules.get(moduleIdx);
            UUID moduleId = contentImportService.createModule(
                    courseId, confirmedModule.moduleName(), moduleIdx + 1);

            for (ConfirmedLecture confirmedLecture : confirmedModule.lectures()) {
                generateLectureWithTasks(state, moduleId, confirmedModule.moduleName(), confirmedLecture);
            }
        }

        return courseId;
    }

    private void generateLectureWithTasks(
            JobState state, UUID moduleId, String moduleName, ConfirmedLecture confirmedLecture) {
        state.currentItem = moduleName + " — " + confirmedLecture.lectureTitle();
        log.info("Job {}: generating '{}'", state.jobId, state.currentItem);

        // First task also creates the lecture.
        String firstTopic = state.technology + ": " + confirmedLecture.lectureTitle();
        var firstContent = contentArchitectService.generateForTopic(firstTopic);
        ImportResult result = contentImportService.importLectureAndTask(moduleId, firstContent);
        state.completedItems++;

        // Additional tasks for the same lecture (taskCount > 1).
        for (int taskVariant = 2; taskVariant <= confirmedLecture.taskCount(); taskVariant++) {
            state.currentItem = moduleName + " — " + confirmedLecture.lectureTitle()
                    + " (task " + taskVariant + "/" + confirmedLecture.taskCount() + ")";
            String additionalTopic = "Additional coding task (variant " + taskVariant + ") for the lecture '"
                    + confirmedLecture.lectureTitle() + "' in " + state.technology
                    + ". Generate a DIFFERENT task that tests a different aspect of the same concept."
                    + " Keep the same lecture content but produce a new task, templateCode, solutionCode, and testCode.";
            var additionalContent = contentArchitectService.generateForTopic(additionalTopic);
            contentImportService.importAdditionalTask(result.lectureId(), additionalContent);
            state.completedItems++;
        }
    }

    // ── Job state ─────────────────────────────────────────────────────────────

    static final class JobState {

        final UUID jobId;
        final String technology;

        @Nullable
        final UUID courseId;

        @Nullable
        final UUID moduleId;

        @Nullable
        final String moduleName;

        @Nullable
        final ConfirmedCurriculum curriculum;

        volatile JobStatus status = JobStatus.RUNNING;
        volatile int totalItems = 0;
        volatile int completedItems = 0;
        volatile String currentItem = "";
        volatile UUID resultCourseId;
        volatile String errorMessage;

        JobState(
                UUID jobId,
                String technology,
                @Nullable UUID courseId,
                @Nullable UUID moduleId,
                @Nullable String moduleName,
                @Nullable ConfirmedCurriculum curriculum) {
            this.jobId = jobId;
            this.technology = technology;
            this.courseId = courseId;
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.curriculum = curriculum;
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
            return new GenerationJobResponse(
                    jobId,
                    technology,
                    status,
                    totalItems,
                    completedItems,
                    currentItem.isBlank() ? null : currentItem,
                    resultCourseId,
                    errorMessage);
        }
    }
}
