package com.javaacademy.platform.ai.client;

import com.javaacademy.platform.common.ApiException;
import org.springframework.http.HttpStatus;

public class LlmException extends ApiException {

    public LlmException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
