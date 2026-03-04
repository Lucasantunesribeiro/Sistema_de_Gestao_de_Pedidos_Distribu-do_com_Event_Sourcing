package com.ordersystem.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SecurityProblemDetailsAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        detail.setTitle("Validation failed");
        detail.setDetail("Validation failed");
        detail.setProperty("errors", errors);
        enrich(detail, null);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            violations.put(propertyPath, violation.getMessage());
        });
        detail.setTitle("Validation failed");
        detail.setDetail("One or more constraint violations occurred");
        detail.setProperty("violations", violations);
        enrich(detail, null);
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Invalid input");
        detail.setDetail(ex.getMessage());
        enrich(detail, null);
        return detail;
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ProblemDetail handleErrorResponse(ErrorResponseException ex) {
        ProblemDetail detail = ex.getBody();
        enrich(detail, null);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        HttpStatus status = resolveStatus(ex);
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(status.getReasonPhrase());
        if (status.is5xxServerError()) {
            detail.setDetail("Unexpected server error");
        } else {
            detail.setDetail(ex.getMessage());
        }
        enrich(detail, request);
        return detail;
    }

    private void enrich(ProblemDetail detail, HttpServletRequest request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null && request != null) {
            correlationId = request.getHeader("X-Correlation-ID");
            if (correlationId == null) {
                correlationId = request.getHeader("correlationId");
            }
        }
        if (correlationId != null) {
            detail.setProperty("correlationId", correlationId);
        }
    }

    private HttpStatus resolveStatus(Exception ex) {
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
