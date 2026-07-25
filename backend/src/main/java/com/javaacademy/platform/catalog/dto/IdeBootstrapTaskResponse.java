package com.javaacademy.platform.catalog.dto;

import java.util.UUID;

/** Trimmed task representation for the IntelliJ plugin tool window. */
public record IdeBootstrapTaskResponse(UUID id, String title, String difficulty, long xpReward) {}
