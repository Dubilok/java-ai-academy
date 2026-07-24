package com.javaacademy.platform.ai.controller;

import com.javaacademy.platform.ai.dto.HintResponse;
import com.javaacademy.platform.ai.service.HintService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class HintController {

    private final HintService hintService;

    @PostMapping("/{taskId}/ai-hint")
    public HintResponse requestHint(@PathVariable UUID taskId, Authentication authentication) {
        String userEmail = authentication.getName();
        return hintService.generateHint(taskId, userEmail);
    }
}
