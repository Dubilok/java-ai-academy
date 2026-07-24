package com.javaacademy.platform.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    HttpServletRequest request;

    GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        given(request.getRequestURI()).willReturn("/api/v1/test");
    }

    // ── ApiException ──────────────────────────────────────────────────────────

    @Test
    void handleApiException_maps404StatusAndMessage() {
        ApiException ex = new ApiException(HttpStatus.NOT_FOUND, "Resource not found");

        ProblemDetail problem = handler.handleApiException(ex, request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Resource not found");
        assertThat(problem.getInstance()).hasToString("/api/v1/test");
    }

    @Test
    void handleApiException_maps409StatusAndMessage() {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "Email already registered");

        ProblemDetail problem = handler.handleApiException(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Email already registered");
    }

    // ── MethodArgumentNotValidException ───────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void handleValidation_mapsFieldErrorsToErrorsProperty() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        given(bindingResult.getFieldErrors())
                .willReturn(List.of(
                        new FieldError("request", "email", "must be a valid email address"),
                        new FieldError("request", "password", "size must be between 8 and 128")));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        given(ex.getBindingResult()).willReturn(bindingResult);

        ProblemDetail problem = handler.handleValidation(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Request validation failed");

        List<GlobalExceptionHandler.FieldViolation> errors = (List<GlobalExceptionHandler.FieldViolation>)
                problem.getProperties().get("errors");
        assertThat(errors).hasSize(2);
        assertThat(errors)
                .extracting(GlobalExceptionHandler.FieldViolation::pointer)
                .containsExactlyInAnyOrder("/email", "/password");
        assertThat(errors)
                .extracting(GlobalExceptionHandler.FieldViolation::message)
                .containsExactlyInAnyOrder("must be a valid email address", "size must be between 8 and 128");
    }

    @Test
    void handleValidation_usesJsonPointerNotation_fieldNamePrefixedWithSlash() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        given(bindingResult.getFieldErrors())
                .willReturn(List.of(new FieldError("req", "refreshToken", "must not be blank")));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        given(ex.getBindingResult()).willReturn(bindingResult);

        ProblemDetail problem = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        List<GlobalExceptionHandler.FieldViolation> errors = (List<GlobalExceptionHandler.FieldViolation>)
                problem.getProperties().get("errors");
        assertThat(errors.get(0).pointer()).isEqualTo("/refreshToken");
    }

    @Test
    void handleValidation_withNoFieldErrors_returnsEmptyErrorsList() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        given(bindingResult.getFieldErrors()).willReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        given(ex.getBindingResult()).willReturn(bindingResult);

        ProblemDetail problem = handler.handleValidation(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        List<?> errors = (List<?>) problem.getProperties().get("errors");
        assertThat(errors).isEmpty();
    }

    // ── HttpMessageNotReadableException ───────────────────────────────────────

    @Test
    void handleNotReadable_returns400WithMalformedBodyDetail() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ProblemDetail problem = handler.handleNotReadable(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Malformed request body");
    }

    // ── ResponseStatusException ───────────────────────────────────────────────

    @Test
    void handleResponseStatus_mapsStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        ProblemDetail problem = handler.handleResponseStatus(ex, request);

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleResponseStatus_withNullReason_fallsBackToStatusPhrase() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT);

        ProblemDetail problem = handler.handleResponseStatus(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Conflict");
    }

    // ── NoHandlerFoundException / NoResourceFoundException ───────────────────

    @Test
    void handleNoHandlerFound_withNoHandlerFoundException_returns404() {
        given(request.getMethod()).willReturn("GET");
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/v1/unknown", HttpHeaders.EMPTY);

        ProblemDetail problem = handler.handleNoHandlerFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).contains("GET").contains("/api/v1/test");
        assertThat(problem.getInstance()).hasToString("/api/v1/test");
    }

    @Test
    void handleNoHandlerFound_withNoResourceFoundException_returns404() {
        given(request.getMethod()).willReturn("GET");
        NoResourceFoundException ex = mock(NoResourceFoundException.class);

        ProblemDetail problem = handler.handleNoHandlerFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).contains("GET").contains("/api/v1/test");
    }

    // ── unexpected Exception ──────────────────────────────────────────────────

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("Database connection refused");

        ProblemDetail problem = handler.handleUnexpected(ex, request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        // Must NOT leak internal exception message
        assertThat(problem.getDetail()).doesNotContain("Database connection refused");
    }
}
