package com.ordersystem.shared.correlation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enhanced correlation ID interceptor for distributed request tracing.
 * Automatically extracts, generates, and propagates correlation IDs across services.
 * 
 * Features:
 * - Extracts correlation ID from incoming requests
 * - Generates new correlation ID if none provided
 * - Sets correlation ID in thread-local context
 * - Injects correlation ID into response headers
 * - Automatic cleanup to prevent memory leaks
 * - Structured logging integration
 */
@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdInterceptor.class);
    
    /**
     * Pre-handle method called before request processing.
     * Extracts or generates correlation ID and sets up context.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Extract correlation ID from request headers
            String correlationId = CorrelationIdUtils.extractFromRequest(request);
            
            if (correlationId == null) {
                // Generate new correlation ID if none provided
                correlationId = CorrelationIdUtils.generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID: {}", correlationId);
            }
            
            // Set correlation ID in thread-local context
            CorrelationIdContext.set(correlationId);
            
            // Inject correlation ID into response headers for client tracking
            CorrelationIdUtils.injectIntoResponse(response);
            
            log.trace("Correlation ID setup complete for request: {} {}", 
                request.getMethod(), request.getRequestURI());
            
        } catch (Exception e) {
            log.warn("Failed to setup correlation ID, generating fallback: {}", e.getMessage());
            // Fallback: generate new correlation ID to ensure tracing continues
            String fallbackId = CorrelationIdUtils.generateCorrelationId();
            CorrelationIdContext.set(fallbackId);
            CorrelationIdUtils.injectIntoResponse(response);
        }
        
        return true;
    }
    
    /**
     * After completion method called after request processing.
     * Cleans up thread-local context to prevent memory leaks.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        try {
            String correlationId = CorrelationIdContext.getOrNull();
            if (correlationId != null) {
                log.trace("Cleaning up correlation ID: {} for request: {} {}", 
                    correlationId, request.getMethod(), request.getRequestURI());
            }
            
            // Always clear context to prevent memory leaks
            CorrelationIdContext.clear();
            
        } catch (Exception e) {
            log.warn("Failed to cleanup correlation ID: {}", e.getMessage());
            // Force cleanup even if logging fails
            CorrelationIdContext.clear();
        }
    }
    
    /**
     * After concurrent handling started method.
     * Ensures correlation ID is preserved for async processing.
     * Note: This method is available in newer Spring versions.
     */
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                              Object handler) {
        // Note: For async processing, correlation ID needs to be manually propagated
        // to the async thread. This is handled by async interceptors or manual context transfer.
        String correlationId = CorrelationIdContext.getOrNull();
        if (correlationId != null) {
            log.debug("Async processing started with correlation ID: {}", correlationId);
        }
    }
}