package com.javaacademy.platform.ai.rag;

import com.javaacademy.platform.common.ApiException;
import org.springframework.http.HttpStatus;

public class VectorStoreException extends ApiException {

    public VectorStoreException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
