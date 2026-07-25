package com.javaacademy.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationIdFilterTest {

    CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    void doFilter_noIncomingHeader_generatesUuidAndSetsOnResponse() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<String> capturedId = new AtomicReference<>();

        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn(null);
        doAnswer(invocation -> {
                    capturedId.set(MDC.get(CorrelationIdFilter.MDC_KEY));
                    return null;
                })
                .when(chain)
                .doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isNotNull().matches("[0-9a-f-]{36}");
    }

    @Test
    void doFilter_incomingHeaderPresent_propagatesExistingId() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<String> capturedId = new AtomicReference<>();

        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("my-trace-id-123");
        doAnswer(invocation -> {
                    capturedId.set(MDC.get(CorrelationIdFilter.MDC_KEY));
                    return null;
                })
                .when(chain)
                .doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isEqualTo("my-trace-id-123");
    }

    @Test
    void doFilter_afterCompletion_mdcKeyIsCleared() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("some-id");

        filter.doFilterInternal(request, response, chain);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doFilter_blankIncomingHeader_generatesNewId() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<String> capturedId = new AtomicReference<>();

        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("  ");
        doAnswer(invocation -> {
                    capturedId.set(MDC.get(CorrelationIdFilter.MDC_KEY));
                    return null;
                })
                .when(chain)
                .doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isNotBlank().isNotEqualTo("  ");
    }
}
