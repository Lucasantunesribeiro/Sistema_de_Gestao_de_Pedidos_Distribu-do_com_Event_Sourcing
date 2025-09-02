package com.ordersystem.unified.shared.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the unified order system.
 * Provides standardized error responses for all exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderSystemException.class)
    public ResponseEntity<ErrorResponse> handleOrderSystemException(OrderSystemException ex, WebRequest request) {
        logger.warn("Business exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            null,
            request.getDescription(false)
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation exception occurred: {}", ex.getMessage());
        
        List<String> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            details,
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            null,
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencyException(ConcurrencyException ex, WebRequest request) {
        logger.warn("Concurrency exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            List.of("Please retry the operation after a short delay"),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "RUNTIME_ERROR",
            "A runtime error occurred: " + ex.getMessage(),
            null,
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            null,
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}