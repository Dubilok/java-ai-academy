package com.javaacademy.platform.ai.client;

public interface LlmClient {
    LlmResponse complete(LlmRequest request);
}
