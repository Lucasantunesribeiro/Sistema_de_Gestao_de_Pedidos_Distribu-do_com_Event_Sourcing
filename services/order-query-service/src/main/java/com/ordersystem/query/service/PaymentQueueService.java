package com.ordersystem.query.service;

import com.ordersystem.query.dto.PaymentRequest;

/**
 * Payment queue service for deferred processing
 */
public interface PaymentQueueService {
    
    void queuePaymentForRetry(PaymentRequest request);
    
    void processQueuedPayments();
}