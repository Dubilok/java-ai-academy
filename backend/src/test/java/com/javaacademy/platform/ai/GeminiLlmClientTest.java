package com.javaacademy.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.ai.client.GeminiLlmClient;
import com.javaacademy.platform.ai.client.LlmException;
import com.javaacademy.platform.ai.client.LlmRequest;
import com.javaacademy.platform.ai.client.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiLlmClientTest {

    static final GeminiProperties PROPS = new GeminiProperties(
            "test-gemini-key", "https://generativelanguage.googleapis.com", "gemini-2.0-flash", 2, 10L);

    static final String GENERATE_CONTENT_URI =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=test-gemini-key";

    static final String SUCCESS_RESPONSE =
            """
            {
              "candidates": [
                {
                  "content": {
                    "role": "model",
                    "parts": [{"text": "Think about what the compiler error actually means."}]
                  },
                  "finishReason": "STOP"
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 120,
                "candidatesTokenCount": 40,
                "totalTokenCount": 160
              }
            }
            """;

    RestClient.Builder restClientBuilder;
    MockRestServiceServer mockServer;
    GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new GeminiLlmClient(restClientBuilder, PROPS, new ObjectMapper());
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void complete_successfulResponse_returnsLlmResponse() {
        mockServer
                .expect(requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request("What should I check first?"));

        assertThat(response.content()).isEqualTo("Think about what the compiler error actually means.");
        assertThat(response.promptTokens()).isEqualTo(120);
        assertThat(response.completionTokens()).isEqualTo(40);
        mockServer.verify();
    }

    @Test
    void complete_nullModelInRequest_usesDefaultModelFromProperties() {
        mockServer
                .expect(requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmRequest requestWithNoModel = new LlmRequest(null, "You are a tutor", "Help me", 256);
        LlmResponse response = client.complete(requestWithNoModel);

        assertThat(response.content()).isNotBlank();
        mockServer.verify();
    }

    // ── retry behaviour ────────────────────────────────────────────────────────

    @Test
    void complete_serverError_retriesAndEventuallySucceeds() {
        mockServer.expect(once(), requestToUriTemplate(GENERATE_CONTENT_URI)).andRespond(withServerError());
        mockServer
                .expect(once(), requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request("Help"));

        assertThat(response.content()).contains("compiler error");
        mockServer.verify();
    }

    @Test
    void complete_rateLimited_retriesAndEventuallySucceeds() {
        mockServer
                .expect(once(), requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer
                .expect(once(), requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request("Help"));

        assertThat(response.content()).contains("compiler error");
        mockServer.verify();
    }

    @Test
    void complete_allRetriesExhausted_throwsLlmException() {
        // maxRetries = 2 → 3 total attempts
        mockServer.expect(times(3), requestToUriTemplate(GENERATE_CONTENT_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(request("Help"))).isInstanceOf(LlmException.class);
        mockServer.verify();
    }

    @Test
    void complete_clientError4xx_doesNotRetryAndThrowsLlmException() {
        mockServer
                .expect(once(), requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.complete(request("Help"))).isInstanceOf(LlmException.class);
        mockServer.verify();
    }

    // ── error handling ─────────────────────────────────────────────────────────

    @Test
    void complete_emptyCandidates_throwsLlmException() {
        String emptyResponse =
                """
                {"candidates": [], "usageMetadata": {"promptTokenCount": 5, "candidatesTokenCount": 0, "totalTokenCount": 5}}
                """;
        mockServer
                .expect(once(), requestToUriTemplate(GENERATE_CONTENT_URI))
                .andRespond(withSuccess(emptyResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(request("Help"))).isInstanceOf(LlmException.class);
        mockServer.verify();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static LlmRequest request(String userPrompt) {
        return new LlmRequest("gemini-2.0-flash", "You are a Socratic tutor.", userPrompt, 256);
    }
}
