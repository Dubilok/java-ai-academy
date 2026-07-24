package com.javaacademy.platform.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.GeminiProperties;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Gemini Generative Language API client behind the {@link LlmClient} interface.
 *
 * <p>Uses the REST API directly (no Google SDK dependency). Endpoint:
 * {@code POST {baseUrl}/v1beta/models/{model}:generateContent?key={apiKey}}
 */
@Slf4j
@Service
public final class GeminiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiLlmClient(
            RestClient.Builder restClientBuilder, GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return withRetry(() -> callApi(request));
    }

    private LlmResponse callApi(LlmRequest request) {
        String model = request.model() != null ? request.model() : properties.defaultModel();
        String uri = "/v1beta/models/" + model + ":generateContent?key=" + properties.apiKey();

        ApiRequest body = buildRequest(request);
        String requestJson = toJson(body);

        String responseJson = restClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);

        return parseResponse(responseJson);
    }

    private ApiRequest buildRequest(LlmRequest request) {
        List<ApiPart> systemParts = List.of(new ApiPart(request.systemPrompt()));
        ApiSystemInstruction systemInstruction = new ApiSystemInstruction(systemParts);

        List<ApiPart> userParts = List.of(new ApiPart(request.userPrompt()));
        ApiContent userContent = new ApiContent("user", userParts);

        ApiGenerationConfig generationConfig = new ApiGenerationConfig(request.maxTokens());

        return new ApiRequest(systemInstruction, List.of(userContent), generationConfig);
    }

    private LlmResponse parseResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new LlmException("Empty response from Gemini API");
        }
        try {
            ApiResponse response = objectMapper.readValue(responseJson, ApiResponse.class);
            if (response.candidates() == null || response.candidates().isEmpty()) {
                throw new LlmException("No candidates in Gemini API response");
            }
            ApiCandidate candidate = response.candidates().get(0);
            if (candidate.content() == null
                    || candidate.content().parts() == null
                    || candidate.content().parts().isEmpty()) {
                throw new LlmException("No content parts in Gemini API candidate");
            }
            String text = candidate.content().parts().stream()
                    .map(ApiPart::text)
                    .findFirst()
                    .orElseThrow(() -> new LlmException("No text in Gemini API response parts"));

            int promptTokens =
                    response.usageMetadata() != null ? response.usageMetadata().promptTokenCount() : 0;
            int completionTokens =
                    response.usageMetadata() != null ? response.usageMetadata().candidatesTokenCount() : 0;
            return new LlmResponse(text, promptTokens, completionTokens);
        } catch (IOException parseException) {
            throw new LlmException("Failed to parse Gemini API response: " + parseException.getMessage());
        }
    }

    private <T> T withRetry(Supplier<T> operation) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            if (attempt > 0) {
                sleepWithJitter(attempt - 1);
            }
            try {
                return operation.get();
            } catch (HttpServerErrorException serverException) {
                lastException = serverException;
                log.warn(
                        "Gemini request failed with server error (attempt {}): {}",
                        attempt + 1,
                        serverException.getStatusCode());
            } catch (HttpClientErrorException clientException) {
                if (clientException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    lastException = clientException;
                    log.warn("Gemini request rate-limited (attempt {}), backing off", attempt + 1);
                } else {
                    throw new LlmException("Gemini request failed: " + clientException.getMessage());
                }
            }
        }
        throw new LlmException(
                "Gemini request failed after " + properties.maxRetries() + " retries: " + lastException.getMessage());
    }

    private void sleepWithJitter(int attempt) {
        long baseDelayMs = properties.retryInitialDelayMs() * (1L << attempt);
        long jitterMs = ThreadLocalRandom.current().nextLong(0, Math.max(1, baseDelayMs / 2));
        try {
            Thread.sleep(baseDelayMs + jitterMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted during retry backoff");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException serializeException) {
            throw new LlmException("Failed to serialize Gemini request: " + serializeException.getMessage());
        }
    }

    // ── Internal Gemini API DTOs ──────────────────────────────────────────────

    private record ApiRequest(
            @JsonProperty("system_instruction") ApiSystemInstruction systemInstruction,
            List<ApiContent> contents,
            @JsonProperty("generationConfig") ApiGenerationConfig generationConfig) {}

    private record ApiSystemInstruction(List<ApiPart> parts) {}

    private record ApiContent(String role, List<ApiPart> parts) {}

    private record ApiPart(String text) {}

    private record ApiGenerationConfig(@JsonProperty("maxOutputTokens") int maxOutputTokens) {}

    private record ApiResponse(List<ApiCandidate> candidates, ApiUsageMetadata usageMetadata) {}

    private record ApiCandidate(ApiContent content, String finishReason) {}

    private record ApiUsageMetadata(int promptTokenCount, int candidatesTokenCount, int totalTokenCount) {}
}
