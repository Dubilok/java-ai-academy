package com.javaacademy.platform.ai.client;

import org.jspecify.annotations.Nullable;

public record LlmRequest(@Nullable String model, String systemPrompt, String userPrompt, int maxTokens) {}
