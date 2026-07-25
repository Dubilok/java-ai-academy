package com.javaacademy.platform.ai.controller;

import com.javaacademy.platform.ai.dto.AiUsageResponse;
import com.javaacademy.platform.ai.dto.GenerateCourseRequest;
import com.javaacademy.platform.ai.dto.GenerationJobResponse;
import com.javaacademy.platform.ai.enums.JobStatus;
import com.javaacademy.platform.ai.service.FinOpsService;
import com.javaacademy.platform.ai.service.GenerationJobService;
import com.javaacademy.platform.common.ApiException;
import jakarta.validation.Valid;
import java.time.Instant;
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
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final GenerationJobService generationJobService;
    private final FinOpsService finOpsService;

    @PostMapping("/generate-course")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationJobResponse startGeneration(@Valid @RequestBody GenerateCourseRequest request) {
        UUID jobId = generationJobService.startJob(request.technology());
        return new GenerationJobResponse(jobId, request.technology(), JobStatus.RUNNING, null, null);
    }

    @GetMapping("/jobs/{jobId}")
    public GenerationJobResponse getJob(@PathVariable UUID jobId) {
        return generationJobService
                .getJob(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    @GetMapping("/usage")
    public AiUsageResponse getUsage(
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to) {
        return finOpsService.getUsage(from, to);
    }
}
