package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all business exceptions in the unified order system.
 * Provides standardized error handling with HTTP status codes.
 */
public abstract class OrderSystemException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] parameters;

    protected OrderSystemException(String message, String errorCode, HttpStatus httpStatus) {
        this(message, errorCode, httpStatus, null, (Object[]) null);
    }

    protected OrderSystemException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        this(message, errorCode, httpStatus, cause, (Object[]) null);
    }

    protected OrderSystemException(String message, String errorCode, HttpStatus httpStatus, Object... parameters) {
        this(message, errorCode, httpStatus, null, parameters);
    }

    protected OrderSystemException(String message, String errorCode, HttpStatus httpStatus, Throwable cause, Object... parameters) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.parameters = parameters;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return String.format("%s{errorCode='%s', httpStatus=%s, message='%s'}", 
                getClass().getSimpleName(), errorCode, httpStatus, getMessage());
    }
}