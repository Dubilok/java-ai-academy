package com.javaacademy.platform.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.BedrockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

class BedrockLlmClientTest {

    static final BedrockProperties PROPS = new BedrockProperties(
            "us-east-1", "anthropic.claude-sonnet-4-5-20250514-v1:0", 2, 10L, 3.0, 15.0, null, null);

    static final BedrockProperties PROPS_WITH_GUARDRAIL = new BedrockProperties(
            "us-east-1", "anthropic.claude-sonnet-4-5-20250514-v1:0", 2, 10L, 3.0, 15.0, "gr-abc123", "1");

    static final String SUCCESS_RESPONSE =
            """
            {
              "content": [{"type": "text", "text": "Here is a Socratic question for you."}],
              "usage": {"input_tokens": 20, "output_tokens": 10},
              "stop_reason": "end_turn"
            }
            """;

    static final String GUARDRAIL_BLOCKED_RESPONSE =
            """
            {
              "content": [],
              "usage": {"input_tokens": 20, "output_tokens": 0},
              "stop_reason": "guardrail_intervened"
            }
            """;

    BedrockRuntimeClient mockBedrockClient;
    BedrockLlmClient client;

    @BeforeEach
    void setUp() {
        mockBedrockClient = mock(BedrockRuntimeClient.class);
        client = new BedrockLlmClient(mockBedrockClient, PROPS, new ObjectMapper());
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void complete_successfulResponse_returnsLlmResponse() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        LlmResponse response = client.complete(request("Explain Java records"));

        assertThat(response.content()).isEqualTo("Here is a Socratic question for you.");
        assertThat(response.promptTokens()).isEqualTo(20);
        assertThat(response.completionTokens()).isEqualTo(10);
    }

    @Test
    void complete_requestUsesProvidedModel() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        LlmRequest requestWithModel =
                new LlmRequest("anthropic.claude-3-haiku-20240307-v1:0", "You are a tutor", "Explain Java", 100);
        client.complete(requestWithModel);

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(mockBedrockClient).invokeModel(captor.capture());
        assertThat(captor.getValue().modelId()).isEqualTo("anthropic.claude-3-haiku-20240307-v1:0");
    }

    @Test
    void complete_nullModelInRequest_usesDefaultModelFromProperties() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        LlmRequest requestWithNoModel = new LlmRequest(null, "You are a tutor", "Explain Java", 100);
        LlmResponse response = client.complete(requestWithNoModel);

        assertThat(response.content()).isNotBlank();
        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(mockBedrockClient).invokeModel(captor.capture());
        assertThat(captor.getValue().modelId()).isEqualTo("anthropic.claude-sonnet-4-5-20250514-v1:0");
    }

    // ── guardrail behaviour ───────────────────────────────────────────────────

    @Test
    void complete_withGuardrailConfig_attachesGuardrailToRequest() {
        BedrockLlmClient clientWithGuardrail =
                new BedrockLlmClient(mockBedrockClient, PROPS_WITH_GUARDRAIL, new ObjectMapper());
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        clientWithGuardrail.complete(request("Explain Java"));

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(mockBedrockClient).invokeModel(captor.capture());
        assertThat(captor.getValue().guardrailIdentifier()).isEqualTo("gr-abc123");
        assertThat(captor.getValue().guardrailVersion()).isEqualTo("1");
    }

    @Test
    void complete_withoutGuardrailConfig_doesNotAttachGuardrail() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        client.complete(request("Explain Java"));

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(mockBedrockClient).invokeModel(captor.capture());
        assertThat(captor.getValue().guardrailIdentifier()).isNull();
    }

    @Test
    void complete_guardrailIntervened_throwsLlmException() {
        BedrockLlmClient clientWithGuardrail =
                new BedrockLlmClient(mockBedrockClient, PROPS_WITH_GUARDRAIL, new ObjectMapper());
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(GUARDRAIL_BLOCKED_RESPONSE));

        assertThatThrownBy(() -> clientWithGuardrail.complete(request("Unsafe content")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("guardrail_intervened");
    }

    @Test
    void complete_guardrailIntervenedStopReason_alwaysRejectedRegardlessOfGuardrailConfig() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse(GUARDRAIL_BLOCKED_RESPONSE));

        assertThatThrownBy(() -> client.complete(request("Content")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("guardrail_intervened");
    }

    // ── retry behaviour ────────────────────────────────────────────────────────

    @Test
    void complete_throttlingException_retriesAndEventuallySucceeds() {
        SdkServiceException throttled = (SdkServiceException) SdkServiceException.builder()
                .statusCode(429)
                .message("ThrottlingException")
                .build();

        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(throttled)
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        LlmResponse response = client.complete(request("Hello"));

        assertThat(response.content()).isEqualTo("Here is a Socratic question for you.");
        verify(mockBedrockClient, times(2)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void complete_serviceError5xx_retriesAndEventuallySucceeds() {
        SdkServiceException serverError = (SdkServiceException) SdkServiceException.builder()
                .statusCode(500)
                .message("InternalServerError")
                .build();

        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(serverError)
                .thenReturn(invokeModelResponse(SUCCESS_RESPONSE));

        LlmResponse response = client.complete(request("Hello"));

        assertThat(response.content()).isEqualTo("Here is a Socratic question for you.");
        verify(mockBedrockClient, times(2)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void complete_allRetriesExhausted_throwsLlmException() {
        SdkServiceException serverError = (SdkServiceException) SdkServiceException.builder()
                .statusCode(500)
                .message("InternalServerError")
                .build();
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class))).thenThrow(serverError);

        assertThatThrownBy(() -> client.complete(request("Hello"))).isInstanceOf(LlmException.class);
        verify(mockBedrockClient, times(3)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void complete_clientError4xx_doesNotRetryAndThrowsLlmException() {
        SdkServiceException clientError = (SdkServiceException) SdkServiceException.builder()
                .statusCode(400)
                .message("ValidationException")
                .build();
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class))).thenThrow(clientError);

        assertThatThrownBy(() -> client.complete(request("Hello"))).isInstanceOf(LlmException.class);
        verify(mockBedrockClient, times(1)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void complete_emptyResponse_throwsLlmException() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(invokeModelResponse(""));

        assertThatThrownBy(() -> client.complete(request("Hello"))).isInstanceOf(LlmException.class);
    }

    @Test
    void complete_malformedJson_throwsLlmException() {
        when(mockBedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(invokeModelResponse("{not-valid-json}"));

        assertThatThrownBy(() -> client.complete(request("Hello"))).isInstanceOf(LlmException.class);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static LlmRequest request(String userPrompt) {
        return new LlmRequest(
                "anthropic.claude-sonnet-4-5-20250514-v1:0", "You are a helpful Java tutor.", userPrompt, 256);
    }

    private static InvokeModelResponse invokeModelResponse(String json) {
        return InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(json)).build();
    }
}
