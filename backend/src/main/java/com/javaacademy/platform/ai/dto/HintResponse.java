package com.javaacademy.platform.ai.dto;

import java.util.UUID;

public record HintResponse(UUID taskId, String hint) {}
