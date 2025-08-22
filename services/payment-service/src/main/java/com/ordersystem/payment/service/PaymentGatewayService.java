package com.ordersystem.payment.service;

import com.ordersystem.payment.exception.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayService.class);

    @Value("${payment.processing.simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${payment.processing.simulation.success-rate:0.9}")
    private double successRate;

    @Value("${payment.processing.simulation.processing-delay:1000}")
    private long processingDelayMs;

    private final Random random = new Random();

    public CompletableFuture<PaymentGatewayResponse> processPayment(String orderId, BigDecimal amount, String paymentMethod) {
        logger.info("Processing payment for order {}, amount: {}, method: {}", orderId, amount, paymentMethod);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate processing delay
                if (simulationEnabled) {
                    TimeUnit.MILLISECONDS.sleep(processingDelayMs);
                }

                // Validate amount
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new PaymentProcessingException(orderId, "Invalid amount: " + amount, "INVALID_AMOUNT");
                }

                // Validate payment method
                if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                    throw new PaymentProcessingException(orderId, "Payment method is required", "MISSING_PAYMENT_METHOD");
                }

                // Simulate payment processing
                if (simulationEnabled) {
                    return simulatePaymentProcessing(orderId, amount, paymentMethod);
                } else {
                    // In a real implementation, this would call the actual payment gateway
                    return processRealPayment(orderId, amount, paymentMethod);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PaymentProcessingException(orderId, "Payment processing interrupted", "PROCESSING_INTERRUPTED");
            } catch (PaymentProcessingException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Unexpected error processing payment for order {}: {}", orderId, e.getMessage(), e);
                throw new PaymentProcessingException(orderId, "Unexpected error during payment processing", "GATEWAY_ERROR");
            }
        });
    }

    private PaymentGatewayResponse simulatePaymentProcessing(String orderId, BigDecimal amount, String paymentMethod) {
        logger.debug("Simulating payment processing for order {}", orderId);

        // Simulate different failure scenarios
        double randomValue = random.nextDouble();

        if (randomValue > successRate) {
            // Simulate various failure reasons
            String[] failureReasons = {
                "Insufficient funds",
                "Card expired",
                "Invalid card number",
                "Transaction declined by bank",
                "Daily limit exceeded"
            };
            
            String[] errorCodes = {
                "INSUFFICIENT_FUNDS",
                "CARD_EXPIRED", 
                "INVALID_CARD",
                "BANK_DECLINED",
                "LIMIT_EXCEEDED"
            };

            int index = random.nextInt(failureReasons.length);
            
            return PaymentGatewayResponse.declined(
                failureReasons[index],
                errorCodes[index]
            );
        }

        // Simulate successful payment
        String transactionId = "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        return PaymentGatewayResponse.approved(transactionId);
    }

    private PaymentGatewayResponse processRealPayment(String orderId, BigDecimal amount, String paymentMethod) {
        // In a real implementation, this would integrate with actual payment gateways
        // like Stripe, PayPal, Square, etc.
        
        logger.info("Processing real payment for order {} (not implemented)", orderId);
        
        // For now, return a successful response
        String transactionId = "REAL_TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentGatewayResponse.approved(transactionId);
    }

    public static class PaymentGatewayResponse {
        private final boolean success;
        private final String transactionId;
        private final String failureReason;
        private final String errorCode;

        private PaymentGatewayResponse(boolean success, String transactionId, String failureReason, String errorCode) {
            this.success = success;
            this.transactionId = transactionId;
            this.failureReason = failureReason;
            this.errorCode = errorCode;
        }

        public static PaymentGatewayResponse approved(String transactionId) {
            return new PaymentGatewayResponse(true, transactionId, null, null);
        }

        public static PaymentGatewayResponse declined(String failureReason, String errorCode) {
            return new PaymentGatewayResponse(false, null, failureReason, errorCode);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getFailureReason() { return failureReason; }
        public String getErrorCode() { return errorCode; }
    }
}