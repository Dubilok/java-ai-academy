package com.javaacademy.platform.ai;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm.bedrock")
public record BedrockProperties(
        String region,
        String defaultModel,
        int maxRetries,
        long retryInitialDelayMs,
        double inputCostPerMillionTokens,
        double outputCostPerMillionTokens,
        @Nullable String guardrailId,
        @Nullable String guardrailVersion) {}
