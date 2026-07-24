package com.javaacademy.platform.progress.dto;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID id, String email, String role, long xpPoints, long crystals, int level, int streak, Instant createdAt) {}
