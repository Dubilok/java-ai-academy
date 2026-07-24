package com.javaacademy.platform.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm.anthropic")
public record AnthropicProperties(
        String apiKey,
        String baseUrl,
        String apiVersion,
        String defaultModel,
        int maxRetries,
        long retryInitialDelayMs) {}
