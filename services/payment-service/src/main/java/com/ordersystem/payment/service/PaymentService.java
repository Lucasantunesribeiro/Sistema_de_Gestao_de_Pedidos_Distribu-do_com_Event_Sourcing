package com.ordersystem.payment.service;

import com.ordersystem.payment.exception.PaymentNotFoundException;
import com.ordersystem.payment.exception.PaymentProcessingException;
import com.ordersystem.payment.exception.BusinessException;
import com.ordersystem.payment.model.Payment;
import com.ordersystem.payment.repository.PaymentRepository;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import com.ordersystem.shared.events.PaymentFailedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentGatewayService gatewayService;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentGateway")
    public CompletableFuture<Payment> processPayment(String orderId, BigDecimal amount, String paymentMethod) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Processing payment for order {}, amount: {}, correlationId: {}", 
                       orderId, amount, correlationId);

            // Validate input
            validatePaymentRequest(orderId, amount, paymentMethod);

            // Check if payment already exists for this order
            if (paymentRepository.existsByOrderId(orderId)) {
                throw new BusinessException("Payment already exists for order: " + orderId, "DUPLICATE_PAYMENT");
            }

            // Create payment record
            Payment payment = new Payment(orderId, amount, paymentMethod, correlationId);
            payment.startProcessing();
            paymentRepository.save(payment);

            logger.debug("Payment record created: {}", payment.getPaymentId());

            // Process payment asynchronously
            return gatewayService.processPayment(orderId, amount, paymentMethod)
                    .thenApply(response -> {
                        try {
                            MDC.put("correlationId", correlationId);
                            return handlePaymentResponse(payment, response);
                        } finally {
                            MDC.clear();
                        }
                    })
                    .exceptionally(throwable -> {
                        try {
                            MDC.put("correlationId", correlationId);
                            return handlePaymentError(payment, throwable);
                        } finally {
                            MDC.clear();
                        }
                    });

        } catch (Exception e) {
            logger.error("Error processing payment for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    public Payment getPayment(String paymentId) {
        String correlationId = MDC.get("correlationId");
        
        logger.debug("Retrieving payment {}, correlationId: {}", paymentId, correlationId);

        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    public Payment getPaymentByOrderId(String orderId) {
        String correlationId = MDC.get("correlationId");
        
        logger.debug("Retrieving payment for order {}, correlationId: {}", orderId, correlationId);

        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public CompletableFuture<Payment> retryPayment(String paymentId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Retrying payment {}, correlationId: {}", paymentId, correlationId);

            Payment payment = getPayment(paymentId);

            if (!payment.canRetry()) {
                throw new BusinessException("Payment cannot be retried: " + paymentId, "RETRY_NOT_ALLOWED");
            }

            payment.incrementRetryCount();
            payment.startProcessing();
            paymentRepository.save(payment);

            return gatewayService.processPayment(payment.getOrderId(), payment.getAmount(), payment.getPaymentMethod())
                    .thenApply(response -> {
                        try {
                            MDC.put("correlationId", correlationId);
                            return handlePaymentResponse(payment, response);
                        } finally {
                            MDC.clear();
                        }
                    })
                    .exceptionally(throwable -> {
                        try {
                            MDC.put("correlationId", correlationId);
                            return handlePaymentError(payment, throwable);
                        } finally {
                            MDC.clear();
                        }
                    });

        } catch (Exception e) {
            logger.error("Error retrying payment {}: {}", paymentId, e.getMessage(), e);
            throw e;
        }
    }

    public void cancelPayment(String paymentId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Cancelling payment {}, correlationId: {}", paymentId, correlationId);

            Payment payment = getPayment(paymentId);
            payment.cancel();
            paymentRepository.save(payment);

            logger.info("Payment {} cancelled successfully", paymentId);

        } catch (Exception e) {
            logger.error("Error cancelling payment {}: {}", paymentId, e.getMessage(), e);
            throw e;
        }
    }

    // Helper methods
    private void validatePaymentRequest(String orderId, BigDecimal amount, String paymentMethod) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new BusinessException("Order ID is required", "MISSING_ORDER_ID");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", "INVALID_AMOUNT");
        }

        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new BusinessException("Payment method is required", "MISSING_PAYMENT_METHOD");
        }
    }

    private Payment handlePaymentResponse(Payment payment, PaymentGatewayService.PaymentGatewayResponse response) {
        String correlationId = payment.getCorrelationId();
        
        try {
            if (response.isSuccess()) {
                payment.approve(response.getTransactionId());
                paymentRepository.save(payment);

                // Publish payment processed event
                PaymentProcessedEvent event = new PaymentProcessedEvent(
                    payment.getOrderId(),
                    payment.getPaymentId(),
                    payment.getStatus().getCode(),
                    payment.getAmount().doubleValue(),
                    LocalDateTime.now()
                );

                eventPublisher.publishPaymentProcessedEvent(event);

                logger.info("Payment {} approved successfully, transaction: {}", 
                           payment.getPaymentId(), response.getTransactionId());

            } else {
                payment.decline(response.getFailureReason(), response.getErrorCode());
                paymentRepository.save(payment);

                // Publish payment failed event
                PaymentFailedEvent event = new PaymentFailedEvent(
                    payment.getOrderId(),
                    payment.getPaymentId(),
                    payment.getAmount(),
                    response.getFailureReason(),
                    response.getErrorCode(),
                    correlationId,
                    null
                );

                eventPublisher.publishPaymentFailedEvent(event);

                logger.info("Payment {} declined: {}", payment.getPaymentId(), response.getFailureReason());
            }

            return payment;

        } catch (Exception e) {
            logger.error("Error handling payment response for payment {}: {}", 
                        payment.getPaymentId(), e.getMessage(), e);
            return handlePaymentError(payment, e);
        }
    }

    private Payment handlePaymentError(Payment payment, Throwable throwable) {
        String correlationId = payment.getCorrelationId();
        
        try {
            String errorMessage = throwable.getMessage();
            String errorCode = "GATEWAY_ERROR";

            if (throwable instanceof PaymentProcessingException) {
                PaymentProcessingException ppe = (PaymentProcessingException) throwable;
                errorCode = ppe.getErrorCode();
            }

            payment.fail(errorMessage, errorCode);
            paymentRepository.save(payment);

            // Publish payment failed event
            PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getOrderId(),
                payment.getPaymentId(),
                payment.getAmount(),
                errorMessage,
                errorCode,
                correlationId,
                null
            );

            eventPublisher.publishPaymentFailedEvent(event);

            logger.error("Payment {} failed: {}", payment.getPaymentId(), errorMessage);

            return payment;

        } catch (Exception e) {
            logger.error("Error handling payment error for payment {}: {}", 
                        payment.getPaymentId(), e.getMessage(), e);
            throw new PaymentProcessingException(payment.getOrderId(), 
                "Failed to handle payment error", "ERROR_HANDLING_FAILED");
        }
    }

    // Fallback method
    public CompletableFuture<Payment> processPaymentFallback(String orderId, BigDecimal amount, 
                                                           String paymentMethod, Exception ex) {
        logger.error("Circuit breaker activated for processPayment, order {}: {}", orderId, ex.getMessage());
        
        CompletableFuture<Payment> future = new CompletableFuture<>();
        future.completeExceptionally(new PaymentProcessingException(orderId, 
            "Payment service is currently unavailable", "SERVICE_UNAVAILABLE"));
        
        return future;
    }
}