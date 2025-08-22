package com.ordersystem.query.service;

import com.ordersystem.query.dto.PaymentRequest;
import com.ordersystem.query.dto.PaymentResult;

import java.util.concurrent.CompletableFuture;

/**
 * Backup payment provider interface
 */
public interface BackupPaymentProvider {
    
    CompletableFuture<PaymentResult> processPayment(PaymentRequest request);
}