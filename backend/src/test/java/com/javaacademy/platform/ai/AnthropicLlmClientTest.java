package com.javaacademy.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.AnthropicLlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AnthropicLlmClientTest {

    static final AnthropicProperties PROPS = new AnthropicProperties(
            "test-api-key", "https://api.anthropic.com", "2023-06-01", "claude-sonnet-4-6", 2, 10L, 3.0, 15.0);

    static final String SUCCESS_RESPONSE =
            """
            {
              "content": [{"type": "text", "text": "Hello, student!"}],
              "usage": {"input_tokens": 15, "output_tokens": 5}
            }
            """;

    RestClient.Builder restClientBuilder;
    MockRestServiceServer mockServer;
    AnthropicLlmClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new AnthropicLlmClient(restClientBuilder, PROPS, new ObjectMapper());
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void complete_successfulResponse_returnsLlmResponse() {
        mockServer
                .expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request("Explain Java records"));

        assertThat(response.content()).isEqualTo("Hello, student!");
        assertThat(response.promptTokens()).isEqualTo(15);
        assertThat(response.completionTokens()).isEqualTo(5);
        mockServer.verify();
    }

    @Test
    void complete_requestSendsApiKeyAndVersionHeaders() {
        mockServer
                .expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        client.complete(request("Hello"));

        mockServer.verify();
    }

    @Test
    void complete_nullModelInRequest_usesDefaultModelFromProperties() {
        mockServer
                .expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        // model = null → should use properties.defaultModel()
        LlmRequest requestWithNoModel = new LlmRequest(null, "You are a tutor", "Explain Java", 100);
        LlmResponse response = client.complete(requestWithNoModel);

        assertThat(response.content()).isNotBlank();
        mockServer.verify();
    }

    // ── retry behaviour ────────────────────────────────────────────────────────

    @Test
    void complete_serverError_retriesAndEventuallySucceeds() {
        mockServer
                .expect(once(), requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());
        mockServer
                .expect(once(), requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request("Hello"));

        assertThat(response.content()).isEqualTo("Hello, student!");
        mockServer.verify();
    }

    @Test
    void complete_rateLimited_retriesAndEventuallySucceeds() {
        mockServer
                .expect(once(), requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer
                .expect(once(), requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request("Hello"));

        assertThat(response.content()).isEqualTo("Hello, student!");
        mockServer.verify();
    }

    @Test
    void complete_allRetriesExhausted_throwsLlmException() {
        // maxRetries = 2, so 3 total attempts (0, 1, 2)
        mockServer
                .expect(times(3), requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(request("Hello"))).isInstanceOf(LlmException.class);
        mockServer.verify();
    }

    @Test
    void complete_clientError4xx_doesNotRetryAndThrowsLlmException() {
        mockServer
                .expect(once(), requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        // 400 is not retryable — should throw immediately without retry
        assertThatThrownBy(() -> client.complete(request("Hello"))).isInstanceOf(LlmException.class);
        mockServer.verify();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static LlmRequest request(String userPrompt) {
        return new LlmRequest("claude-sonnet-4-6", "You are a helpful Java tutor.", userPrompt, 256);
    }
}
