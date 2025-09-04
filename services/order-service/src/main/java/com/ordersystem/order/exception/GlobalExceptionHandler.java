package com.ordersystem.order.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for Order Service.
 * Provides consistent JSON responses and correlation IDs for troubleshooting.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String ensureCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        String correlationId = ensureCorrelationId();
        logger.warn("⚠️ Invalid request: path={}, error={}, correlationId={}",
                request.getDescription(false), ex.getMessage(), correlationId);

        Map<String, Object> body = Map.of(
                "success", false,
                "error", "Invalid argument",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString(),
                "path", request.getDescription(false),
                "correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, WebRequest request) {
        String correlationId = ensureCorrelationId();
        logger.error("❌ Unhandled exception: path={}, error={}, correlationId={}",
                request.getDescription(false), ex.getMessage(), correlationId, ex);

        Map<String, Object> body = Map.of(
                "success", false,
                "error", "Internal server error",
                "message", "An unexpected error occurred in the order service",
                "timestamp", LocalDateTime.now().toString(),
                "path", request.getDescription(false),
                "correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
