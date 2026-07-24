package com.javaacademy.platform.ai.client;

public record LlmResponse(String content, int promptTokens, int completionTokens) {}
