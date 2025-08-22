package com.ordersystem.query.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Enterprise-grade Audit Logging for Security Events
 * COMPLIANCE TARGET: 100% complete audit trail for business operations
 */
@Component
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    @Autowired
    private ObjectMapper objectMapper;

    @EventListener
    public void logSecurityEvent(SecurityAuditEvent event) {
        try {
            AuditEntry entry = AuditEntry.builder()
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .correlationId(event.getCorrelationId())
                .timestamp(Instant.now())
                .clientIp(event.getClientIp())
                .userAgent(event.getUserAgent())
                .details(event.getDetails())
                .severity(event.getSeverity())
                .build();
                
            auditLog.info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            auditLog.error("Failed to log audit event", e);
        }
    }

    /**
     * Log authentication events
     */
    public void logAuthenticationEvent(String eventType, String userId, String clientIp, 
                                     Map<String, Object> details) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(eventType)
            .userId(userId)
            .clientIp(clientIp)
            .details(details)
            .severity("INFO")
            .build();
            
        logSecurityEvent(event);
    }

    /**
     * Log authorization events
     */
    public void logAuthorizationEvent(String eventType, String userId, String resource, 
                                    boolean allowed, Map<String, Object> details) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(eventType)
            .userId(userId)
            .details(Map.of(
                "resource", resource,
                "allowed", allowed,
                "additionalDetails", details
            ))
            .severity(allowed ? "INFO" : "WARN")
            .build();
            
        logSecurityEvent(event);
    }

    /**
     * Log rate limiting events
     */
    public void logRateLimitEvent(String userId, String clientIp, int requestCount, int limit) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType("RATE_LIMIT_EXCEEDED")
            .userId(userId)
            .clientIp(clientIp)
            .details(Map.of(
                "requestCount", requestCount,
                "limit", limit,
                "timeWindow", "1 minute"
            ))
            .severity("WARN")
            .build();
            
        logSecurityEvent(event);
    }

    /**
     * Log token events
     */
    public void logTokenEvent(String eventType, String userId, Map<String, Object> details) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(eventType)
            .userId(userId)
            .details(details)
            .severity("INFO")
            .build();
            
        logSecurityEvent(event);
    }

    /**
     * Structured audit entry
     */
    public static class AuditEntry {
        private String eventType;
        private String userId;
        private String correlationId;
        private Instant timestamp;
        private String clientIp;
        private String userAgent;
        private Map<String, Object> details;
        private String severity;

        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public static class Builder {
            private final AuditEntry entry = new AuditEntry();

            public Builder eventType(String eventType) {
                entry.setEventType(eventType);
                return this;
            }

            public Builder userId(String userId) {
                entry.setUserId(userId);
                return this;
            }

            public Builder correlationId(String correlationId) {
                entry.setCorrelationId(correlationId);
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                entry.setTimestamp(timestamp);
                return this;
            }

            public Builder clientIp(String clientIp) {
                entry.setClientIp(clientIp);
                return this;
            }

            public Builder userAgent(String userAgent) {
                entry.setUserAgent(userAgent);
                return this;
            }

            public Builder details(Map<String, Object> details) {
                entry.setDetails(details);
                return this;
            }

            public Builder severity(String severity) {
                entry.setSeverity(severity);
                return this;
            }

            public AuditEntry build() {
                return entry;
            }
        }
    }
}