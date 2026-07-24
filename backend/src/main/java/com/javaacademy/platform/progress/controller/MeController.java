package com.javaacademy.platform.progress.controller;

import com.javaacademy.platform.progress.dto.MeResponse;
import com.javaacademy.platform.progress.dto.ProgressSummaryResponse;
import com.javaacademy.platform.progress.service.MeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping("/me")
    public MeResponse getMe(Authentication authentication) {
        return meService.getMe(authentication.getName());
    }

    @GetMapping("/me/progress")
    public ProgressSummaryResponse getProgress(Authentication authentication) {
        return meService.getProgress(authentication.getName());
    }
}
