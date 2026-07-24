package com.javaacademy.platform.progress.controller;

import com.javaacademy.platform.progress.dto.SubmissionResponse;
import com.javaacademy.platform.progress.dto.SubmitCodeRequest;
import com.javaacademy.platform.progress.dto.SubmitResponse;
import com.javaacademy.platform.progress.service.SubmissionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/tasks/{taskId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmitResponse submitCode(
            @PathVariable UUID taskId, @Valid @RequestBody SubmitCodeRequest request, Authentication authentication) {
        return submissionService.createSubmission(authentication.getName(), taskId, request.source());
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionResponse getSubmission(@PathVariable UUID submissionId, Authentication authentication) {
        return submissionService.findSubmission(submissionId, authentication.getName());
    }
}
