package com.ordersystem.shared.correlation;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Thread-local context manager for correlation IDs.
 * Provides O(1) access to correlation ID within the same thread.
 * Automatically manages MDC integration for structured logging.
 */
public final class CorrelationIdContext {
    
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();
    
    /**
     * Sets the correlation ID for the current thread.
     * Automatically updates MDC for logging integration.
     * 
     * @param correlationId the correlation ID to set
     * @throws IllegalArgumentException if correlationId is null or exceeds max length
     */
    public static void set(String correlationId) {
        if (correlationId == null) {
            throw new IllegalArgumentException("Correlation ID cannot be null");
        }
        
        if (correlationId.length() > CorrelationConstants.CORRELATION_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("Correlation ID exceeds maximum length of " + 
                CorrelationConstants.CORRELATION_ID_MAX_LENGTH);
        }
        
        CORRELATION_ID.set(correlationId);
        MDC.put(CorrelationConstants.CORRELATION_ID_MDC_KEY, correlationId);
    }
    
    /**
     * Gets the correlation ID for the current thread.
     * Returns default correlation ID if none is set for backward compatibility.
     * 
     * @return the correlation ID for this thread, never null
     */
    public static String get() {
        String correlationId = CORRELATION_ID.get();
        return correlationId != null ? correlationId : CorrelationConstants.DEFAULT_CORRELATION_ID;
    }
    
    /**
     * Gets the correlation ID for the current thread, or null if not set.
     * Use this when you need to check if correlation ID is actually present.
     * 
     * @return the correlation ID for this thread, or null if not set
     */
    public static String getOrNull() {
        return CORRELATION_ID.get();
    }
    
    /**
     * Generates and sets a new correlation ID for the current thread.
     * Uses UUID v4 for guaranteed uniqueness.
     * 
     * @return the generated correlation ID
     */
    public static String generate() {
        String correlationId = UUID.randomUUID().toString();
        set(correlationId);
        return correlationId;
    }
    
    /**
     * Clears the correlation ID for the current thread.
     * Also removes from MDC to prevent memory leaks.
     * Should be called after request completion.
     */
    public static void clear() {
        CORRELATION_ID.remove();
        MDC.remove(CorrelationConstants.CORRELATION_ID_MDC_KEY);
    }
    
    /**
     * Checks if a correlation ID is set for the current thread.
     * 
     * @return true if correlation ID is present, false otherwise
     */
    public static boolean isPresent() {
        return CORRELATION_ID.get() != null;
    }
    
    /**
     * Sets correlation ID only if none is currently set.
     * Useful for preserving existing correlation IDs.
     * 
     * @param correlationId the correlation ID to set if none exists
     * @return true if correlation ID was set, false if one already existed
     */
    public static boolean setIfAbsent(String correlationId) {
        if (!isPresent()) {
            set(correlationId);
            return true;
        }
        return false;
    }
    
    // Private constructor to prevent instantiation
    private CorrelationIdContext() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}