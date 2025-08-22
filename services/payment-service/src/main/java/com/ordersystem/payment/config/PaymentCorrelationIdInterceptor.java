package com.ordersystem.payment.config;

import com.ordersystem.shared.correlation.CorrelationIdInterceptor;
import org.springframework.stereotype.Component;

/**
 * Payment Service specific correlation ID interceptor.
 * Extends the shared interceptor for service-specific customizations.
 */
@Component
public class PaymentCorrelationIdInterceptor extends CorrelationIdInterceptor {
    // Inherits all functionality from shared interceptor
    // Can be extended for payment-service specific requirements
}