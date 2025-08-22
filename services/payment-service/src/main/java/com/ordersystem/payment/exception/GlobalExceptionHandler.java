package com.ordersystem.payment.exception;

import com.ordersystem.shared.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Payment not found: {}, correlationId: {}", ex.getMessage(), correlationId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            "PAYMENT_NOT_FOUND",
            request.getDescription(false),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessing(PaymentProcessingException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Payment processing error: {}, correlationId: {}", ex.getMessage(), correlationId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Payment Processing Failed",
            ex.getMessage(),
            "PAYMENT_PROCESSING_ERROR",
            request.getDescription(false),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Business exception: {}, correlationId: {}", ex.getMessage(), correlationId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Business Rule Violation",
            ex.getMessage(),
            "BUSINESS_ERROR",
            request.getDescription(false),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Validation error: {}, correlationId: {}", ex.getMessage(), correlationId);
        
        Map<String, List<String>> validationErrors = new HashMap<>();
        
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String fieldName = fieldError.getField();
            String errorMessage = fieldError.getDefaultMessage();
            
            validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        }
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Dados de entrada inválidos",
            "VALIDATION_ERROR",
            request.getDescription(false),
            correlationId
        );
        error.setValidationErrors(validationErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MessageBrokerException.class)
    public ResponseEntity<ErrorResponse> handleMessageBroker(MessageBrokerException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Message broker error: {}, correlationId: {}", ex.getMessage(), correlationId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            "Erro na comunicação entre serviços. Tente novamente em alguns instantes.",
            "MESSAGE_BROKER_ERROR",
            request.getDescription(false),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Unexpected error: {}, correlationId: {}", ex.getMessage(), correlationId, ex);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Erro interno do servidor. Nossa equipe foi notificada.",
            "INTERNAL_ERROR",
            request.getDescription(false),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}