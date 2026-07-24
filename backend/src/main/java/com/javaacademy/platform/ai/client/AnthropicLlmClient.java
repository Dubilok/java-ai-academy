package com.javaacademy.platform.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.AnthropicProperties;
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

@Slf4j
@Service
public final class AnthropicLlmClient implements LlmClient {

    private static final String MESSAGES_PATH = "/v1/messages";

    private final RestClient restClient;
    private final AnthropicProperties properties;
    private final ObjectMapper objectMapper;

    public AnthropicLlmClient(
            RestClient.Builder restClientBuilder, AnthropicProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", properties.apiVersion())
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return withRetry(() -> callApi(request));
    }

    private LlmResponse callApi(LlmRequest request) {
        String model = request.model() != null ? request.model() : properties.defaultModel();
        ApiRequest body = new ApiRequest(
                model,
                request.maxTokens(),
                request.systemPrompt(),
                List.of(new ApiMessage("user", request.userPrompt())));

        String requestJson = toJson(body);
        String responseJson = restClient
                .post()
                .uri(MESSAGES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);

        return parseResponse(responseJson);
    }

    private LlmResponse parseResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new LlmException("Empty response from Anthropic API");
        }
        try {
            ApiResponse response = objectMapper.readValue(responseJson, ApiResponse.class);
            String text = response.content().stream()
                    .filter(block -> "text".equals(block.type()))
                    .map(ApiContentBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new LlmException("No text block in Anthropic API response"));
            return new LlmResponse(
                    text, response.usage().inputTokens(), response.usage().outputTokens());
        } catch (IOException parseException) {
            throw new LlmException("Failed to parse Anthropic API response: " + parseException.getMessage());
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
                        "LLM request failed with server error (attempt {}): {}",
                        attempt + 1,
                        serverException.getStatusCode());
            } catch (HttpClientErrorException clientException) {
                if (clientException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    lastException = clientException;
                    log.warn("LLM request rate-limited (attempt {}), backing off", attempt + 1);
                } else {
                    throw new LlmException("LLM request failed: " + clientException.getMessage());
                }
            }
        }
        throw new LlmException(
                "LLM request failed after " + properties.maxRetries() + " retries: " + lastException.getMessage());
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
            throw new LlmException("Failed to serialize LLM request: " + serializeException.getMessage());
        }
    }

    // ── Internal Anthropic API DTOs ───────────────────────────────────────────

    private record ApiRequest(
            String model, @JsonProperty("max_tokens") int maxTokens, String system, List<ApiMessage> messages) {}

    private record ApiMessage(String role, String content) {}

    private record ApiResponse(List<ApiContentBlock> content, ApiUsage usage) {}

    private record ApiContentBlock(String type, String text) {}

    private record ApiUsage(
            @JsonProperty("input_tokens") int inputTokens, @JsonProperty("output_tokens") int outputTokens) {}
}
