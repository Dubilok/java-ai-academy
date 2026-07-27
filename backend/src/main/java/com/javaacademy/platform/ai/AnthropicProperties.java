package com.javaacademy.platform.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm.anthropic")
public record AnthropicProperties(
        String apiKey,
        String baseUrl,
        String apiVersion,
        String defaultModel,
        /** Model used for first-pass content generation (cheap tier, e.g. Haiku). Falls back to defaultModel if blank. */
        String generationModel,
        /** Model used for self-healing retries (quality tier, e.g. Sonnet). Falls back to defaultModel if blank. */
        String healingModel,
        int maxRetries,
        long retryInitialDelayMs,
        double inputCostPerMillionTokens,
        double outputCostPerMillionTokens) {

    /** Returns generationModel if set, otherwise defaultModel. */
    public String effectiveGenerationModel() {
        return (generationModel != null && !generationModel.isBlank()) ? generationModel : defaultModel;
    }

    /** Returns healingModel if set, otherwise defaultModel. */
    public String effectiveHealingModel() {
        return (healingModel != null && !healingModel.isBlank()) ? healingModel : defaultModel;
    }
}
