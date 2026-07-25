package com.javaacademy.platform.ai.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.BedrockProperties;
import com.javaacademy.platform.ai.client.LlmException;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

/**
 * AWS Bedrock Titan Embeddings V2 implementation of {@link EmbeddingClient}.
 *
 * <p>Active when {@code app.llm.active-provider=bedrock}. Uses model
 * {@code amazon.titan-embed-text-v2:0} which produces 1024-dimensional vectors.
 * Credentials are resolved by the default AWS credential chain — never stored in config.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.llm.active-provider", havingValue = "bedrock")
public final class BedrockEmbeddingClient implements EmbeddingClient {

    private static final int VECTOR_DIMENSIONS = 1024;
    private static final String TITAN_EMBED_MODEL = "amazon.titan-embed-text-v2:0";

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    public BedrockEmbeddingClient(BedrockProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    BedrockEmbeddingClient(BedrockRuntimeClient bedrockClient, ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public int dimensions() {
        return VECTOR_DIMENSIONS;
    }

    @Override
    public float[] embed(String text) {
        String requestJson = toJson(new ApiRequest(text, VECTOR_DIMENSIONS, true));

        InvokeModelRequest sdkRequest = InvokeModelRequest.builder()
                .modelId(TITAN_EMBED_MODEL)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestJson))
                .build();

        String responseJson = bedrockClient.invokeModel(sdkRequest).body().asUtf8String();
        return parseEmbedding(responseJson);
    }

    private float[] parseEmbedding(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new LlmException("Empty embedding response from Bedrock Titan");
        }
        try {
            ApiResponse response = objectMapper.readValue(responseJson, ApiResponse.class);
            List<Double> embedding = response.embedding();
            if (embedding == null || embedding.isEmpty()) {
                throw new LlmException("No embedding in Bedrock Titan response");
            }
            float[] result = new float[embedding.size()];
            for (int index = 0; index < embedding.size(); index++) {
                result[index] = embedding.get(index).floatValue();
            }
            return result;
        } catch (IOException parseException) {
            throw new LlmException("Failed to parse Bedrock Titan embedding response: " + parseException.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException serializeException) {
            throw new LlmException("Failed to serialize Bedrock embedding request: " + serializeException.getMessage());
        }
    }

    private record ApiRequest(@JsonProperty("inputText") String inputText, int dimensions, boolean normalize) {}

    private record ApiResponse(List<Double> embedding) {}
}
