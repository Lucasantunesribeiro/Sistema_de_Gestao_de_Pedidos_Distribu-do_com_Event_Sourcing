package com.ordersystem.query.service;

import com.ordersystem.query.dto.PaymentRequest;
import com.ordersystem.query.dto.PaymentResult;

/**
 * External payment service interface
 */
public interface ExternalPaymentService {
    
    PaymentResult processPayment(PaymentRequest request);
    
    String getPaymentStatus(String orderId);
}