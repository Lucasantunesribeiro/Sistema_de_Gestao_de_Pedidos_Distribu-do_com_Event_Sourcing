package com.ordersystem.shared.correlation;

import com.rabbitmq.client.AMQP;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.UUID;

/**
 * Utility methods for correlation ID extraction, injection, and validation.
 * Provides consistent handling across HTTP, RabbitMQ, and other protocols.
 */
public final class CorrelationIdUtils {
    
    /**
     * Extracts correlation ID from HTTP request headers.
     * Returns null if header is not present or empty.
     * 
     * @param request the HTTP request
     * @return correlation ID from header, or null if not present
     */
    public static String extractFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        String correlationId = request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER);
        return isValidCorrelationId(correlationId) ? correlationId : null;
    }
    
    /**
     * Extracts correlation ID from HTTP headers map.
     * 
     * @param headers the HTTP headers map
     * @return correlation ID from headers, or null if not present
     */
    public static String extractFromHeaders(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        
        String correlationId = headers.getFirst(CorrelationConstants.CORRELATION_ID_HEADER);
        return isValidCorrelationId(correlationId) ? correlationId : null;
    }
    
    /**
     * Injects correlation ID into HTTP response headers.
     * Uses current thread's correlation ID if available.
     * 
     * @param response the HTTP response
     */
    public static void injectIntoResponse(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        
        String correlationId = CorrelationIdContext.getOrNull();
        if (correlationId != null) {
            response.setHeader(CorrelationConstants.CORRELATION_ID_HEADER, correlationId);
        }
    }
    
    /**
     * Injects correlation ID into HTTP headers.
     * Uses current thread's correlation ID if available.
     * 
     * @param headers the HTTP headers to modify
     */
    public static void injectIntoHeaders(HttpHeaders headers) {
        if (headers == null) {
            return;
        }
        
        String correlationId = CorrelationIdContext.getOrNull();
        if (correlationId != null) {
            headers.set(CorrelationConstants.CORRELATION_ID_HEADER, correlationId);
        }
    }
    
    /**
     * Extracts correlation ID from RabbitMQ message properties.
     * 
     * @param properties the message properties
     * @return correlation ID from message, or null if not present
     */
    public static String extractFromMessage(MessageProperties properties) {
        if (properties == null || properties.getHeaders() == null) {
            return null;
        }
        
        Object correlationId = properties.getHeaders().get(CorrelationConstants.CORRELATION_ID_MESSAGE_PROPERTY);
        if (correlationId instanceof String) {
            return isValidCorrelationId((String) correlationId) ? (String) correlationId : null;
        }
        
        return null;
    }
    
    /**
     * Injects correlation ID into RabbitMQ message properties.
     * Uses current thread's correlation ID if available.
     * 
     * @param properties the message properties to modify
     */
    public static void injectIntoMessage(MessageProperties properties) {
        if (properties == null) {
            return;
        }
        
        String correlationId = CorrelationIdContext.getOrNull();
        if (correlationId != null) {
            if (properties.getHeaders() == null) {
                properties.setHeader(CorrelationConstants.CORRELATION_ID_MESSAGE_PROPERTY, correlationId);
            } else {
                properties.getHeaders().put(CorrelationConstants.CORRELATION_ID_MESSAGE_PROPERTY, correlationId);
            }
        }
    }
    
    /**
     * Creates SQL comment with correlation ID for database tracing.
     * 
     * @return SQL comment string with correlation ID
     */
    public static String createSqlComment() {
        String correlationId = CorrelationIdContext.getOrNull();
        if (correlationId != null) {
            return String.format(CorrelationConstants.CORRELATION_ID_SQL_COMMENT, correlationId);
        }
        return "";
    }
    
    /**
     * Validates if a string is a valid correlation ID.
     * Checks for null, empty, and length constraints.
     * 
     * @param correlationId the correlation ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCorrelationId(String correlationId) {
        return correlationId != null && 
               !correlationId.trim().isEmpty() && 
               correlationId.length() <= CorrelationConstants.CORRELATION_ID_MAX_LENGTH;
    }
    
    /**
     * Generates a new UUID-based correlation ID.
     * Does not set it in context - use CorrelationIdContext.generate() for that.
     * 
     * @return a new correlation ID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Ensures a correlation ID is present, generating one if needed.
     * Returns existing correlation ID or generates a new one.
     * 
     * @param existingId the existing correlation ID (may be null)
     * @return a valid correlation ID (never null)
     */
    public static String ensureCorrelationId(String existingId) {
        return isValidCorrelationId(existingId) ? existingId : generateCorrelationId();
    }
    
    // Private constructor to prevent instantiation
    private CorrelationIdUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}