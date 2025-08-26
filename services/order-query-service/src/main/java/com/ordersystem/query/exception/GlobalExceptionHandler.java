package com.ordersystem.query.exception;

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
 * Global exception handler for Query Service
 * Provides consistent error responses and comprehensive logging
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }

    logger.error("❌ Unhandled exception in Query Service: path={}, error={}, correlationId={}",
        request.getDescription(false), ex.getMessage(), correlationId, ex);

    Map<String, Object> errorResponse = Map.of(
        "success", false,
        "error", "Internal server error",
        "message", "An unexpected error occurred in the query service",
        "timestamp", LocalDateTime.now().toString(),
        "path", request.getDescription(false),
        "correlationId", correlationId);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex,
      WebRequest request) {
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }

    logger.warn("⚠️ Invalid argument in Query Service: path={}, error={}, correlationId={}",
        request.getDescription(false), ex.getMessage(), correlationId);

    Map<String, Object> errorResponse = Map.of(
        "success", false,
        "error", "Invalid argument",
        "message", ex.getMessage(),
        "timestamp", LocalDateTime.now().toString(),
        "path", request.getDescription(false),
        "correlationId", correlationId);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }

    logger.error("❌ Runtime exception in Query Service: path={}, error={}, correlationId={}",
        request.getDescription(false), ex.getMessage(), correlationId, ex);

    Map<String, Object> errorResponse = Map.of(
        "success", false,
        "error", "Runtime error",
        "message", ex.getMessage(),
        "timestamp", LocalDateTime.now().toString(),
        "path", request.getDescription(false),
        "correlationId", correlationId);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}