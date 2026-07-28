package com.familytree.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesAndReturnsANewCorrelationIdWhenNoneSupplied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String header = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(header).isNotBlank();
        assertThat(UUID.fromString(header)).isNotNull();
    }

    @Test
    void reusesAnIncomingCorrelationIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "upstream-request-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("upstream-request-id-123");
    }

    @Test
    void populatesMdcDuringTheRequestAndClearsItAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcValueDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(mdcValueDuringChain.get()).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcEvenWhenTheChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                filter.doFilter(request, response, (req, res) -> {
                    throw new RuntimeException("boom");
                }));

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
