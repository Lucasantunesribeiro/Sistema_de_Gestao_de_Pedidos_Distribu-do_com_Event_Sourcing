package com.ordersystem.query.security;

import java.util.Map;

/**
 * Security audit event for structured logging
 */
public class SecurityAuditEvent {
    private String eventType;
    private String userId;
    private String correlationId;
    private String clientIp;
    private String userAgent;
    private Map<String, Object> details;
    private String severity;

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getEventType() { return eventType; }
    public String getUserId() { return userId; }
    public String getCorrelationId() { return correlationId; }
    public String getClientIp() { return clientIp; }
    public String getUserAgent() { return userAgent; }
    public Map<String, Object> getDetails() { return details; }
    public String getSeverity() { return severity; }

    // Setters
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public void setSeverity(String severity) { this.severity = severity; }

    public static class Builder {
        private final SecurityAuditEvent event = new SecurityAuditEvent();

        public Builder eventType(String eventType) {
            event.setEventType(eventType);
            return this;
        }

        public Builder userId(String userId) {
            event.setUserId(userId);
            return this;
        }

        public Builder correlationId(String correlationId) {
            event.setCorrelationId(correlationId);
            return this;
        }

        public Builder clientIp(String clientIp) {
            event.setClientIp(clientIp);
            return this;
        }

        public Builder userAgent(String userAgent) {
            event.setUserAgent(userAgent);
            return this;
        }

        public Builder details(Map<String, Object> details) {
            event.setDetails(details);
            return this;
        }

        public Builder severity(String severity) {
            event.setSeverity(severity);
            return this;
        }

        public SecurityAuditEvent build() {
            return event;
        }
    }
}