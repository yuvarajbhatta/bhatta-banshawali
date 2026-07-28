package com.familytree.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation id to every request -- reusing one supplied by an
 * upstream caller/proxy via {@value #CORRELATION_ID_HEADER} if present, or
 * generating a new one otherwise -- and puts it in the MDC so every log line
 * for that request carries it (structured logging includes MDC context
 * automatically; see logging.structured.json.context.include). Registered
 * ahead of the Spring Security filter chain (see {@link LoggingConfig}) so
 * even requests Security redirects or rejects still get one.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            logRequestCompletion(request, response, startNanos);
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, long startNanos) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/actuator/prometheus")) {
            return;
        }

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("{} {} -> {} ({} ms)", request.getMethod(), uri, response.getStatus(), durationMs);
    }
}
