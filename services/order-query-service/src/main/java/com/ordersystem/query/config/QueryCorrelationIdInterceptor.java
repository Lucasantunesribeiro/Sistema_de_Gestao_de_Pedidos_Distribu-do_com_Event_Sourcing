package com.ordersystem.query.config;

import com.ordersystem.shared.correlation.CorrelationIdInterceptor;
import org.springframework.stereotype.Component;

/**
 * Query Service specific correlation ID interceptor.
 * Extends the shared interceptor for service-specific customizations.
 */
@Component
public class QueryCorrelationIdInterceptor extends CorrelationIdInterceptor {
    // Inherits all functionality from shared interceptor
    // Can be extended for query-service specific requirements
}