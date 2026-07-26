package com.javaacademy.platform.ai.controller;

import com.javaacademy.platform.ai.dto.AiUsageResponse;
import com.javaacademy.platform.ai.dto.EvaluationSummary;
import com.javaacademy.platform.ai.dto.GenerateCourseRequest;
import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.ai.service.EvaluationDashboardService;
import com.javaacademy.platform.ai.service.FinOpsService;
import com.javaacademy.platform.ai.service.GenerationJobService;
import com.javaacademy.platform.catalog.dto.CourseResponse;
import com.javaacademy.platform.catalog.dto.ModuleResponse;
import com.javaacademy.platform.catalog.service.CatalogService;
import com.javaacademy.platform.common.ApiException;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAiController {

    private final GenerationJobService generationJobService;
    private final FinOpsService finOpsService;
    private final EvaluationDashboardService evaluationDashboardService;
    private final CatalogService catalogService;

    @PostMapping("/ai/generate-course")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationJobResponse startGeneration(@Valid @RequestBody GenerateCourseRequest request) {
        UUID jobId = generationJobService.startJob(request);
        return new GenerationJobResponse(jobId, request.technology(), JobStatus.RUNNING, null, null);
    }

    @GetMapping("/ai/jobs/{jobId}")
    public GenerationJobResponse getJob(@PathVariable UUID jobId) {
        return generationJobService
                .getJob(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    @GetMapping("/ai/usage")
    public AiUsageResponse getUsage(
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to) {
        return finOpsService.getUsage(from, to);
    }

    @GetMapping("/ai/evaluations")
    public List<EvaluationSummary> getEvaluations(@RequestParam(required = false) @Nullable String targetType) {
        return evaluationDashboardService.getRecentEvaluations(targetType);
    }

    /** Lists all courses (published and unpublished) for the admin content picker. */
    @GetMapping("/courses")
    public List<CourseResponse> listAllCourses() {
        return catalogService.listAllCourses();
    }

    /** Lists modules for a given course, for the admin content picker. */
    @GetMapping("/courses/{courseId}/modules")
    public List<ModuleResponse> listModules(@PathVariable UUID courseId) {
        return catalogService.listModulesForCourse(courseId);
    }

    /** Publishes or unpublishes a course. */
    @PostMapping("/courses/{courseId}/publish")
    public CourseResponse togglePublish(@PathVariable UUID courseId) {
        return catalogService.togglePublish(courseId);
    }
}
