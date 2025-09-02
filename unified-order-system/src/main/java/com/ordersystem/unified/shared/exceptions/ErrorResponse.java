package com.ordersystem.unified.shared.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response format for API endpoints.
 */
public class ErrorResponse {
    
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;
    
    @JsonProperty("errorCode")
    private final String errorCode;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("details")
    private final List<String> details;
    
    @JsonProperty("path")
    private final String path;

    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public ErrorResponse(String errorCode, String message, List<String> details) {
        this(errorCode, message, details, null);
    }

    public ErrorResponse(String errorCode, String message, List<String> details, String path) {
        this.timestamp = LocalDateTime.now();
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.path = path;
    }

    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public List<String> getDetails() { return details; }
    public String getPath() { return path; }
}