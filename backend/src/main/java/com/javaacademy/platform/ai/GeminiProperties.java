package com.javaacademy.platform.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm.gemini")
public record GeminiProperties(
        String apiKey, String baseUrl, String defaultModel, int maxRetries, long retryInitialDelayMs) {}
