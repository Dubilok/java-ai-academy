package com.javaacademy.platform.catalog.dto;

import java.util.UUID;

public record TaskStubResponse(UUID id, String title, String difficulty, long xpReward) {}
