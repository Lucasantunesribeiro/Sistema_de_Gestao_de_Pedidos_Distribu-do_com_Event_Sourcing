package com.ordersystem.shared.correlation;

import org.springframework.stereotype.Service;

/**
 * Example demonstrating how to use the enhanced correlation ID infrastructure.
 * Shows migration from manual MDC usage to automatic correlation tracking.
 */
@Service
public class CorrelationExample {
    
    // OLD WAY - Manual MDC management
    /*
    private static final Logger logger = LoggerFactory.getLogger(CorrelationExample.class);
    
    public void oldWayExample() {
        String correlationId = MDC.get("correlationId");
        logger.info("Processing order with correlation: {}", correlationId);
    }
    */
    
    // NEW WAY - Enhanced correlation infrastructure
    private static final CorrelatedLogger log = CorrelatedLogger.getLogger(CorrelationExample.class);
    
    /**
     * Example of enhanced service method with automatic correlation tracking.
     */
    public void enhancedServiceMethod() {
        // Correlation ID is automatically available through context
        String correlationId = CorrelationIdContext.get();
        
        // Structured logging with automatic correlation inclusion
        log.info("Processing order for customer: {}", "customerId123");
        log.debug("Order validation completed successfully");
        
        // Call other services - correlation ID is automatically propagated
        callExternalService();
        
        // Publish events - correlation ID is automatically included
        publishOrderEvent();
    }
    
    /**
     * Example of calling external services with automatic correlation propagation.
     */
    private void callExternalService() {
        // When using RestTemplate or WebClient, 
        // use CorrelationIdUtils.injectIntoHeaders() to automatically add correlation ID
        
        log.debug("Calling payment service");
        
        // The interceptor automatically injects correlation ID into outgoing requests
        // No manual header management needed
    }
    
    /**
     * Example of publishing events with correlation tracking.
     */
    private void publishOrderEvent() {
        // When publishing to RabbitMQ, use CorrelationIdMessagePostProcessor
        // to automatically inject correlation ID into message properties
        
        log.debug("Publishing order created event");
        
        // The message post-processor automatically includes correlation ID
        // No manual message property management needed
    }
    
    /**
     * Example of conditional correlation ID generation.
     */
    public void conditionalCorrelationExample() {
        // Check if correlation ID exists
        if (CorrelationIdContext.isPresent()) {
            log.info("Using existing correlation ID");
        } else {
            // Generate new correlation ID if needed
            String newId = CorrelationIdContext.generate();
            log.info("Generated new correlation ID: {}", newId);
        }
    }
    
    /**
     * Example of safe correlation ID handling.
     */
    public void safeCorrelationExample() {
        // Always safe - returns default if not set
        String correlationId = CorrelationIdContext.get();
        
        // Or check explicitly
        String explicitCheck = CorrelationIdContext.getOrNull();
        if (explicitCheck != null) {
            log.info("Found correlation ID: {}", explicitCheck);
        } else {
            log.warn("No correlation ID found in context");
        }
    }
    
    /**
     * Example of manual correlation ID management for async operations.
     */
    public void asyncCorrelationExample() {
        // Capture correlation ID before async operation
        String correlationId = CorrelationIdContext.getOrNull();
        
        // In async thread, manually set correlation ID
        // CompletableFuture.supplyAsync(() -> {
        //     try {
        //         if (correlationId != null) {
        //             CorrelationIdContext.set(correlationId);
        //         }
        //         
        //         log.info("Processing in async thread");
        //         return "result";
        //         
        //     } finally {
        //         CorrelationIdContext.clear();
        //     }
        // });
    }
}