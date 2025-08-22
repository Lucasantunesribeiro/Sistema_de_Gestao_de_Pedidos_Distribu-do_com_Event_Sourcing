package com.ordersystem.order.config;

import com.ordersystem.shared.correlation.CorrelationIdInterceptor;
import org.springframework.stereotype.Component;

/**
 * Order Service specific correlation ID interceptor.
 * Extends the shared interceptor for service-specific customizations.
 */
@Component
public class OrderCorrelationIdInterceptor extends CorrelationIdInterceptor {
    // Inherits all functionality from shared interceptor
    // Can be extended for order-service specific requirements
}