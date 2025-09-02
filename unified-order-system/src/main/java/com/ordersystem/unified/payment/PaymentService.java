package com.ordersystem.unified.payment;

import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import com.ordersystem.unified.shared.events.PaymentProcessedEvent;
import com.ordersystem.unified.shared.exceptions.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing payments in the unified system.
 * Handles payment creation, processing, and status management with database persistence.
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Processes a payment for an order with synchronous processing.
     */
    public PaymentResult processPayment(String orderId, BigDecimal amount, String correlationId) {
        return processPayment(orderId, amount, correlationId, "CREDIT_CARD");
    }

    /**
     * Processes a payment for an order with specified payment method.
     */
    public PaymentResult processPayment(String orderId, BigDecimal amount, String correlationId, String paymentMethod) {
        logger.info("Processing payment for order: {}, amount: {}, method: {}, correlationId: {}", 
                   orderId, amount, paymentMethod, correlationId);

        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent() && existingPayment.get().isCompleted()) {
            logger.warn("Payment already completed for order: {}, paymentId: {}", 
                       orderId, existingPayment.get().getId());
            return PaymentResult.success(existingPayment.get().getId());
        }

        // Create new payment record
        String paymentId = UUID.randomUUID().toString();
        Payment payment = new Payment(paymentId, orderId, amount, paymentMethod);
        payment.setCorrelationId(correlationId);

        try {
            // Save payment in pending state
            payment = paymentRepository.save(payment);
            logger.debug("Payment record created: {}", payment.getId());

            // Simulate payment processing
            PaymentResult result = simulatePaymentProcessing(payment);

            // Update payment status based on result
            if (result.isSuccess()) {
                payment.markAsCompleted(result.getPaymentId());
                logger.info("Payment successful for order: {}, paymentId: {}, transactionId: {}, correlationId: {}", 
                           orderId, payment.getId(), result.getPaymentId(), correlationId);
            } else {
                payment.markAsFailed(result.getMessage(), result.getErrorCode());
                logger.warn("Payment failed for order: {}, paymentId: {}, reason: {}, correlationId: {}", 
                           orderId, payment.getId(), result.getMessage(), correlationId);
            }

            // Save updated payment
            payment = paymentRepository.save(payment);

            // Publish payment processed event
            publishPaymentProcessedEvent(payment, correlationId);

            return result.isSuccess() ? 
                PaymentResult.success(payment.getId()) : 
                PaymentResult.failure(result.getMessage(), result.getErrorCode());

        } catch (Exception e) {
            logger.error("Payment processing failed for order: {}, paymentId: {}, correlationId: {}", 
                        orderId, paymentId, correlationId, e);
            
            // Mark payment as failed if it exists
            if (payment.getId() != null) {
                payment.markAsFailed("Payment processing error: " + e.getMessage(), "PROCESSING_ERROR");
                paymentRepository.save(payment);
            }
            
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves payment by order ID.
     */
    public Optional<Payment> getPaymentByOrderId(String orderId) {
        logger.debug("Retrieving payment for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Retrieves payment by payment ID.
     */
    public Optional<Payment> getPaymentById(String paymentId) {
        logger.debug("Retrieving payment: {}", paymentId);
        return paymentRepository.findById(paymentId);
    }

    /**
     * Retrieves all payments for an order (including failed attempts).
     */
    public List<Payment> getAllPaymentsByOrderId(String orderId) {
        logger.debug("Retrieving all payments for order: {}", orderId);
        return paymentRepository.findAllByOrderId(orderId);
    }

    /**
     * Checks if a successful payment exists for an order.
     */
    public boolean hasSuccessfulPayment(String orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, 
            com.ordersystem.unified.shared.events.PaymentStatus.COMPLETED);
    }

    // Private helper methods

    private PaymentResult simulatePaymentProcessing(Payment payment) {
        try {
            // Simulate processing time
            Thread.sleep(100);
            
            // For demo purposes, simulate 95% success rate
            if (Math.random() < 0.95) {
                String transactionId = "txn_" + UUID.randomUUID().toString().substring(0, 8);
                return PaymentResult.success(transactionId);
            } else {
                return PaymentResult.failure("Insufficient funds", "INSUFFICIENT_FUNDS");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentResult.failure("Payment processing interrupted", "PROCESSING_ERROR");
        }
    }

    private void publishPaymentProcessedEvent(Payment payment, String correlationId) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            payment.getId(),
            payment.getOrderId(),
            null, // customerId - would need to be passed or looked up
            payment.getAmount(),
            payment.getStatus().name(),
            payment.getTransactionId(),
            correlationId,
            null
        );

        logger.debug("Payment processed event: {}", event);
        // In a real implementation, this could be published to an event bus
        // For now, it's just logged for internal tracking
    }
}