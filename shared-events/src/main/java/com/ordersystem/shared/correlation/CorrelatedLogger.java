package com.ordersystem.shared.correlation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced logger that automatically includes correlation ID in log messages.
 * Provides structured logging with consistent correlation tracking.
 */
public class CorrelatedLogger {
    
    private final Logger logger;
    
    private CorrelatedLogger(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Creates a correlated logger for the specified class.
     * 
     * @param clazz the class for which to create the logger
     * @return a new CorrelatedLogger instance
     */
    public static CorrelatedLogger getLogger(Class<?> clazz) {
        return new CorrelatedLogger(LoggerFactory.getLogger(clazz));
    }
    
    /**
     * Creates a correlated logger with the specified name.
     * 
     * @param name the logger name
     * @return a new CorrelatedLogger instance
     */
    public static CorrelatedLogger getLogger(String name) {
        return new CorrelatedLogger(LoggerFactory.getLogger(name));
    }
    
    // Debug methods
    public void debug(String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(addCorrelation(message));
        }
    }
    
    public void debug(String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(addCorrelation(format), args);
        }
    }
    
    // Info methods
    public void info(String message) {
        if (logger.isInfoEnabled()) {
            logger.info(addCorrelation(message));
        }
    }
    
    public void info(String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(addCorrelation(format), args);
        }
    }
    
    // Warn methods
    public void warn(String message) {
        logger.warn(addCorrelation(message));
    }
    
    public void warn(String format, Object... args) {
        logger.warn(addCorrelation(format), args);
    }
    
    public void warn(String message, Throwable throwable) {
        logger.warn(addCorrelation(message), throwable);
    }
    
    // Error methods
    public void error(String message) {
        logger.error(addCorrelation(message));
    }
    
    public void error(String format, Object... args) {
        logger.error(addCorrelation(format), args);
    }
    
    public void error(String message, Throwable throwable) {
        logger.error(addCorrelation(message), throwable);
    }
    
    // Trace methods
    public void trace(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(addCorrelation(message));
        }
    }
    
    public void trace(String format, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(addCorrelation(format), args);
        }
    }
    
    // Level check methods
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }
    
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }
    
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }
    
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
    
    /**
     * Gets the underlying SLF4J logger for advanced usage.
     * 
     * @return the underlying logger
     */
    public Logger getUnderlying() {
        return logger;
    }
    
    /**
     * Adds correlation ID to log message if available.
     * Uses MDC integration for structured logging.
     * 
     * @param message the original message
     * @return message with correlation context
     */
    private String addCorrelation(String message) {
        String correlationId = CorrelationIdContext.getOrNull();
        if (correlationId != null) {
            return String.format("[correlationId=%s] %s", correlationId, message);
        }
        return message;
    }
}