package com.ordersystem.unified.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to automatically record metrics for HTTP requests.
 * Records request count, success/failure rates, and duration.
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MetricsInterceptor.class);

    private final MeterRegistry meterRegistry;

    public MetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Store start time in request attribute
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Calculate duration
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;

            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            // Record HTTP metrics
            Timer.builder("http.server.requests.custom")
                    .description("Custom HTTP request metrics")
                    .tag("method", method)
                    .tag("uri", sanitizeUri(uri))
                    .tag("status", String.valueOf(status))
                    .tag("outcome", getOutcome(status))
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

            // Record specific business operation metrics
            recordBusinessMetrics(uri, method, status);
        }
    }

    private void recordBusinessMetrics(String uri, String method, int status) {
        boolean isSuccess = status >= 200 && status < 300;

        // Order operations
        if (uri.contains("/api/orders") && method.equals("POST")) {
            incrementCounter("order.creation.total");
            if (isSuccess) {
                incrementCounter("order.creation.success");
            } else {
                incrementCounter("order.creation.failed");
            }
        } else if (uri.contains("/api/orders") && uri.contains("/cancel") && method.equals("PUT")) {
            incrementCounter("order.cancellation.total");
        }

        // Payment operations
        if (uri.contains("/api/payments") && method.equals("POST")) {
            incrementCounter("payment.attempts.total");
            if (isSuccess) {
                incrementCounter("payment.success.total");
            } else {
                incrementCounter("payment.failed.total");
            }
        }

        // Inventory operations
        if (uri.contains("/api/inventory/reserve") && method.equals("POST")) {
            incrementCounter("inventory.reservation.total");
            if (isSuccess) {
                incrementCounter("inventory.reservation.success");
            } else {
                incrementCounter("inventory.reservation.failed");
            }
        }
    }

    private void incrementCounter(String name) {
        try {
            Counter.builder(name)
                    .description("Auto-recorded business metric")
                    .tag("source", "interceptor")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            logger.debug("Failed to increment counter: {}", name, e);
        }
    }

    private String sanitizeUri(String uri) {
        // Replace UUIDs and IDs with placeholders to reduce cardinality
        return uri
                .replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "{uuid}")
                .replaceAll("/\\d+", "/{id}");
    }

    private String getOutcome(int status) {
        if (status >= 200 && status < 300) {
            return "SUCCESS";
        } else if (status >= 300 && status < 400) {
            return "REDIRECTION";
        } else if (status >= 400 && status < 500) {
            return "CLIENT_ERROR";
        } else if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "UNKNOWN";
    }
}
