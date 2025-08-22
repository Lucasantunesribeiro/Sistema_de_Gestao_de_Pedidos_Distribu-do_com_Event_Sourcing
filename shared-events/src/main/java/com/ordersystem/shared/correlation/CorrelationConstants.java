package com.ordersystem.shared.correlation;

/**
 * Constants for correlation ID management across all microservices.
 * Provides standardized header names and MDC keys for distributed tracing.
 */
public final class CorrelationConstants {
    
    /**
     * HTTP header name for correlation ID.
     * Used for incoming and outgoing HTTP requests.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    /**
     * MDC key for correlation ID in logging context.
     * Used by Logback/SLF4J for structured logging.
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    /**
     * RabbitMQ message property key for correlation ID.
     * Used for event tracing across message queues.
     */
    public static final String CORRELATION_ID_MESSAGE_PROPERTY = "correlationId";
    
    /**
     * SQL comment prefix for database correlation.
     * Used for database query tracing.
     */
    public static final String CORRELATION_ID_SQL_COMMENT = "/* correlationId: %s */";
    
    /**
     * Maximum length for correlation ID to prevent overflow.
     * Standard UUID length: 36 characters.
     */
    public static final int CORRELATION_ID_MAX_LENGTH = 36;
    
    /**
     * Default correlation ID when none is provided.
     * Used for backward compatibility.
     */
    public static final String DEFAULT_CORRELATION_ID = "unknown";
    
    // Private constructor to prevent instantiation
    private CorrelationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}