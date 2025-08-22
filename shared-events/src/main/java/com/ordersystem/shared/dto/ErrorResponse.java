package com.ordersystem.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ErrorResponse {
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("status")
    private int status;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("validationErrors")
    private Map<String, List<String>> validationErrors;

    // Default constructor
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String error, String message, String code) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.code = code;
    }

    public ErrorResponse(int status, String error, String message, String code, 
                        String path, String correlationId) {
        this(status, error, message, code);
        this.path = path;
        this.correlationId = correlationId;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public Map<String, List<String>> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(Map<String, List<String>> validationErrors) { 
        this.validationErrors = validationErrors; 
    }
}