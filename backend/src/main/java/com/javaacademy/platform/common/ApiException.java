package com.javaacademy.platform.common;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain exceptions. Subclass per domain when a named type adds clarity;
 * throw directly for one-off cases. The global handler maps this to RFC 7807 ProblemDetail.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
