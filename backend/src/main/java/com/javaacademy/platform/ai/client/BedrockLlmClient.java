package com.javaacademy.platform.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.BedrockProperties;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * AWS Bedrock route for the LlmClient interface.
 *
 * <p>Active when {@code app.llm.active-provider=bedrock}. Calls the Bedrock InvokeModel API with
 * Claude-compatible request/response bodies (same JSON shape as the Anthropic Messages API, plus
 * {@code anthropic_version: "bedrock-2023-05-31"}). AWS credentials are resolved by the default
 * credential chain (env vars, instance profile, etc.) — never stored in config.
 *
 * <p>When {@code app.llm.bedrock.guardrail-id} and {@code guardrail-version} are set, the guardrail
 * is attached to every request. If Bedrock reports {@code stop_reason=guardrail_intervened} the
 * response is rejected with an {@link LlmException} so callers never receive filtered content.
 */
@Slf4j
@Primary
@Service
@ConditionalOnProperty(name = "app.llm.active-provider", havingValue = "bedrock")
public final class BedrockLlmClient implements LlmClient {

    private static final String ANTHROPIC_BEDROCK_VERSION = "bedrock-2023-05-31";

    private final BedrockRuntimeClient bedrockClient;
    private final BedrockProperties properties;
    private final ObjectMapper objectMapper;

    public BedrockLlmClient(BedrockProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    BedrockLlmClient(BedrockRuntimeClient bedrockClient, BedrockProperties properties, ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return withRetry(() -> callApi(request));
    }

    private static final String GUARDRAIL_INTERVENED = "guardrail_intervened";

    private LlmResponse callApi(LlmRequest request) {
        String model = request.model() != null ? request.model() : properties.defaultModel();
        ApiRequest body = new ApiRequest(
                ANTHROPIC_BEDROCK_VERSION,
                request.maxTokens(),
                request.systemPrompt(),
                List.of(new ApiMessage("user", request.userPrompt())));

        String requestJson = toJson(body);

        InvokeModelRequest.Builder sdkRequestBuilder = InvokeModelRequest.builder()
                .modelId(model)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestJson));

        String guardrailId = properties.guardrailId();
        String guardrailVersion = properties.guardrailVersion();
        if (guardrailId != null && !guardrailId.isBlank() && guardrailVersion != null && !guardrailVersion.isBlank()) {
            sdkRequestBuilder.guardrailIdentifier(guardrailId).guardrailVersion(guardrailVersion);
            log.debug("Applying Bedrock guardrail {} v{}", guardrailId, guardrailVersion);
        }

        InvokeModelResponse sdkResponse = bedrockClient.invokeModel(sdkRequestBuilder.build());
        return parseResponse(sdkResponse.body().asUtf8String());
    }

    private LlmResponse parseResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new LlmException("Empty response from Bedrock API");
        }
        try {
            ApiResponse response = objectMapper.readValue(responseJson, ApiResponse.class);
            if (GUARDRAIL_INTERVENED.equals(response.stopReason())) {
                throw new LlmException("Bedrock Guardrail blocked the response (stop_reason=guardrail_intervened)");
            }
            String text = response.content().stream()
                    .filter(block -> "text".equals(block.type()))
                    .map(ApiContentBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new LlmException("No text block in Bedrock API response"));
            return new LlmResponse(
                    text, response.usage().inputTokens(), response.usage().outputTokens());
        } catch (IOException parseException) {
            throw new LlmException("Failed to parse Bedrock API response: " + parseException.getMessage());
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
            } catch (SdkServiceException sdkException) {
                int statusCode = sdkException.statusCode();
                if (statusCode == 429 || statusCode >= 500) {
                    lastException = sdkException;
                    log.warn("Bedrock request failed (attempt {}, status {}), will retry", attempt + 1, statusCode);
                } else {
                    throw new LlmException("Bedrock request failed: " + sdkException.getMessage());
                }
            }
        }
        throw new LlmException(
                "Bedrock request failed after " + properties.maxRetries() + " retries: " + lastException.getMessage());
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
            throw new LlmException("Failed to serialize Bedrock request: " + serializeException.getMessage());
        }
    }

    // ── Internal Bedrock/Claude API DTOs (same shape as Anthropic Messages API) ─

    private record ApiRequest(
            @JsonProperty("anthropic_version") String anthropicVersion,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<ApiMessage> messages) {}

    private record ApiMessage(String role, String content) {}

    private record ApiResponse(
            List<ApiContentBlock> content, ApiUsage usage, @JsonProperty("stop_reason") String stopReason) {}

    private record ApiContentBlock(String type, String text) {}

    private record ApiUsage(
            @JsonProperty("input_tokens") int inputTokens, @JsonProperty("output_tokens") int outputTokens) {}
}
