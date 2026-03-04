package com.ordersystem.unified.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to add correlation ID to all requests.
 * Extracts correlation ID from header or generates a new one.
 * Adds it to MDC for structured logging and to response headers.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID
            String correlationId = extractCorrelationId(httpRequest);

            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Generate trace and span IDs for distributed tracing
            String traceId = generateTraceId();
            String spanId = generateSpanId();
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            MDC.put(SPAN_ID_MDC_KEY, spanId);

            // Add to response headers
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader("X-Trace-ID", traceId);
            httpResponse.setHeader("X-Span-ID", spanId);

            // Log request
            logRequest(httpRequest, correlationId, traceId);

            // Continue with the request
            long startTime = System.currentTimeMillis();
            chain.doFilter(request, response);
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(httpRequest, httpResponse, duration, correlationId);

        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    private String extractCorrelationId(HttpServletRequest request) {
        // Try to get from header
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // Try alternative headers
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader("X-Request-ID");
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader("X-Correlation-Id");
        }

        // Generate new if not present
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }

        return correlationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 16).replace("-", "");
    }

    private void logRequest(HttpServletRequest request, String correlationId, String traceId) {
        if (logger.isDebugEnabled()) {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String remoteAddr = request.getRemoteAddr();

            logger.debug("Incoming request: method={}, uri={}, query={}, remoteAddr={}, correlationId={}, traceId={}",
                    method, uri, query, remoteAddr, correlationId, traceId);
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response,
                             long duration, String correlationId) {
        if (logger.isDebugEnabled()) {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            logger.debug("Response: method={}, uri={}, status={}, duration={}ms, correlationId={}",
                    method, uri, status, duration, correlationId);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CorrelationIdFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("CorrelationIdFilter destroyed");
    }
}
