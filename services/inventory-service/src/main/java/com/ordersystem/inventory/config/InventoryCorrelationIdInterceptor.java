package com.ordersystem.inventory.config;

import com.ordersystem.shared.correlation.CorrelationIdInterceptor;
import org.springframework.stereotype.Component;

/**
 * Inventory Service specific correlation ID interceptor.
 * Extends the shared interceptor for service-specific customizations.
 */
@Component
public class InventoryCorrelationIdInterceptor extends CorrelationIdInterceptor {
    // Inherits all functionality from shared interceptor
    // Can be extended for inventory-service specific requirements
}