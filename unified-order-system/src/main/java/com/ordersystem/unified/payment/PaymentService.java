package com.ordersystem.unified.payment;

import com.ordersystem.unified.infrastructure.events.EventPublisher;
import com.ordersystem.unified.payment.dto.CustomerInfo;
import com.ordersystem.unified.payment.dto.PaymentRequest;
import com.ordersystem.unified.payment.dto.PaymentResponse;
import com.ordersystem.unified.payment.dto.PaymentStatus;
import com.ordersystem.unified.payment.gateway.PaymentGatewayClient;
import com.ordersystem.unified.payment.gateway.PaymentGatewayRefundRequest;
import com.ordersystem.unified.payment.gateway.PaymentGatewayRequest;
import com.ordersystem.unified.payment.gateway.PaymentGatewayResponse;
import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import com.ordersystem.unified.domain.events.PaymentProcessedEvent;
import com.ordersystem.unified.domain.events.PaymentRefundedEvent;
import com.ordersystem.unified.shared.exceptions.PaymentProcessingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service for processing payments in the unified system.
 * Handles payment creation, processing, and status management with database persistence.
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("100000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final EventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGatewayClient paymentGatewayClient,
                          EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Processes a payment using PaymentRequest DTO.
     */
    @CacheEvict(cacheNames = "dashboard", allEntries = true)
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info("Processing payment request for order: {}, amount: {}, method: {}",
                   request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        validateAmount(request.getAmount());
        try {
            ProcessedPayment processedPayment = executeCharge(
                request.getOrderId(),
                request.getAmount(),
                request.getCorrelationId(),
                request.getPaymentMethod() != null ? request.getPaymentMethod().name() : "CREDIT_CARD",
                request.getCustomerInfo()
            );

            return buildResponse(processedPayment.payment(), processedPayment.gatewayResponse());
        } catch (RuntimeException exception) {
            logger.error("Payment request processing failed for order: {}", request.getOrderId(), exception);
            throw new PaymentProcessingException("Payment processing failed: " + exception.getMessage(), exception);
        }
    }

    /**
     * Processes a payment for an order with synchronous processing.
     */
    public PaymentResult processPayment(String orderId, BigDecimal amount, String correlationId) {
        return processPayment(orderId, amount, correlationId, "CREDIT_CARD");
    }

    /**
     * Processes a payment for an order with specified payment method.
     */
    @CacheEvict(cacheNames = "dashboard", allEntries = true)
    public PaymentResult processPayment(String orderId, BigDecimal amount, String correlationId, String paymentMethod) {
        logger.info("Processing payment for order: {}, amount: {}, method: {}, correlationId: {}", 
                   orderId, amount, paymentMethod, correlationId);

        validateAmount(amount);

        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent() && existingPayment.get().isCompleted()) {
            logger.warn("Payment already completed for order: {}, paymentId: {}", 
                       orderId, existingPayment.get().getId());
            return PaymentResult.success(existingPayment.get().getId());
        }

        final Payment payment;
        final PaymentGatewayResponse gatewayResponse;
        try {
            ProcessedPayment processedPayment = executeCharge(orderId, amount, correlationId, paymentMethod, null);
            payment = processedPayment.payment();
            gatewayResponse = processedPayment.gatewayResponse();
        } catch (RuntimeException exception) {
            logger.error("Payment processing failed for order: {}", orderId, exception);
            throw new PaymentProcessingException("Payment processing failed: " + exception.getMessage(), exception);
        }

        if (payment.isCompleted()) {
            logger.info("Payment successful for order: {}, paymentId: {}, transactionId: {}, correlationId: {}",
                orderId, payment.getId(), payment.getTransactionId(), correlationId);
            return PaymentResult.success(payment.getId());
        }

        if (com.ordersystem.unified.domain.events.PaymentStatus.PENDING.equals(payment.getStatus())) {
            logger.info("Payment pending for order: {}, paymentId: {}, correlationId: {}",
                orderId, payment.getId(), correlationId);
            return PaymentResult.pending(payment.getId());
        }

        logger.warn("Payment failed for order: {}, paymentId: {}, reason: {}, correlationId: {}",
            orderId, payment.getId(), gatewayResponse.getMessage(), correlationId);
        return PaymentResult.failure(gatewayResponse.getMessage(), gatewayResponse.getErrorCode());
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
     * Processes a refund for a completed payment.
     * Marks the payment as REFUNDED and returns the refund transaction ID.
     *
     * @param paymentId ID of the payment to refund
     * @param reason    Human-readable refund reason
     * @return refund transaction ID if successful, empty otherwise
     */
    @CacheEvict(cacheNames = "dashboard", allEntries = true)
    public Optional<String> refundPayment(String paymentId, String reason) {
        logger.info("Processing refund for payment: {}, reason: {}", paymentId, reason);

        return paymentRepository.findById(paymentId).flatMap(payment -> {
            if (!payment.isCompleted()) {
                logger.warn("Cannot refund payment in status {}: paymentId={}", payment.getStatus(), paymentId);
                return Optional.<String>empty();
            }

            PaymentGatewayRefundRequest request = new PaymentGatewayRefundRequest();
            request.setPaymentId(payment.getId());
            request.setOrderId(payment.getOrderId());
            request.setAmount(payment.getAmount());
            request.setCorrelationId(payment.getCorrelationId());
            request.setReason(reason);

            PaymentGatewayResponse gatewayResponse = paymentGatewayClient.refund(request);
            if (gatewayResponse == null) {
                logger.warn("Gateway returned an empty refund response for paymentId={}", paymentId);
                return Optional.<String>empty();
            }
            String normalizedStatus = normalizeStatus(gatewayResponse.getStatus());
            if (!"REFUNDED".equals(normalizedStatus) && !gatewayResponse.isApproved()) {
                logger.warn("Gateway rejected refund for paymentId={}: {}", paymentId, gatewayResponse.getMessage());
                return Optional.<String>empty();
            }

            payment.markAsRefunded(gatewayResponse.getTransactionId());
            paymentRepository.save(payment);
            publishPaymentRefundedEvent(payment, reason, gatewayResponse.getTransactionId());

            logger.info("Payment refunded: paymentId={}, refundTxId={}", paymentId, gatewayResponse.getTransactionId());
            return Optional.ofNullable(gatewayResponse.getTransactionId());
        });
    }

    /**
     * Checks if a successful payment exists for an order.
     */
    public boolean hasSuccessfulPayment(String orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, 
            com.ordersystem.unified.domain.events.PaymentStatus.COMPLETED);
    }

    // Private helper methods

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

        eventPublisher.publishWithinTransaction(event);
        logger.debug("Payment processed event persisted to outbox: {}", event);
    }

    private void publishPaymentRefundedEvent(Payment payment, String reason, String refundTransactionId) {
        PaymentRefundedEvent event = new PaymentRefundedEvent(
            payment.getId(),
            payment.getOrderId(),
            null,
            payment.getAmount(),
            payment.getAmount(),
            reason,
            payment.getCorrelationId()
        );
        event.setRefundTransactionId(refundTransactionId);
        eventPublisher.publishWithinTransaction(event);
        logger.debug("Payment refunded event persisted to outbox: paymentId={}", payment.getId());
    }

    private ProcessedPayment executeCharge(String orderId,
                                           BigDecimal amount,
                                           String correlationId,
                                           String paymentMethod,
                                           CustomerInfo customerInfo) {
        Payment payment = new Payment(UUID.randomUUID().toString(), orderId, amount, paymentMethod);
        payment.setCorrelationId(correlationId);
        payment = paymentRepository.save(payment);

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest();
        gatewayRequest.setOrderId(orderId);
        gatewayRequest.setAmount(amount);
        gatewayRequest.setPaymentMethod(paymentMethod);
        gatewayRequest.setCorrelationId(correlationId);
        if (customerInfo != null) {
            gatewayRequest.setCustomerId(customerInfo.getCustomerId());
            gatewayRequest.setCustomerName(customerInfo.getCustomerName());
            gatewayRequest.setCustomerEmail(customerInfo.getCustomerEmail());
        }

        PaymentGatewayResponse gatewayResponse;
        try {
            gatewayResponse = paymentGatewayClient.charge(gatewayRequest);
            if (gatewayResponse == null) {
                gatewayResponse = gatewayFailureResponse(new IllegalStateException("Gateway returned an empty response"));
            }
        } catch (Exception exception) {
            logger.error("Payment gateway call failed for order: {}", orderId, exception);
            gatewayResponse = gatewayFailureResponse(exception);
        }

        applyGatewayResponse(payment, gatewayResponse);
        payment = paymentRepository.save(payment);
        publishPaymentProcessedEvent(payment, correlationId);
        return new ProcessedPayment(payment, gatewayResponse);
    }

    private void applyGatewayResponse(Payment payment, PaymentGatewayResponse gatewayResponse) {
        String normalizedStatus = normalizeStatus(gatewayResponse.getStatus());
        String message = StringUtils.hasText(gatewayResponse.getMessage())
            ? gatewayResponse.getMessage()
            : "Payment processing failed";

        if ("COMPLETED".equals(normalizedStatus) && gatewayResponse.isApproved()) {
            payment.markAsCompleted(gatewayResponse.getTransactionId());
            return;
        }

        if ("PENDING".equals(normalizedStatus)) {
            payment.markAsPending();
            payment.setTransactionId(gatewayResponse.getTransactionId());
            return;
        }

        payment.markAsFailed(message, gatewayResponse.getErrorCode());
    }

    private PaymentResponse buildResponse(Payment payment, PaymentGatewayResponse gatewayResponse) {
        PaymentResponse response = new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            PaymentStatus.valueOf(payment.getStatus().name()),
            payment.getAmount(),
            payment.getTransactionId(),
            LocalDateTime.now(),
            payment.getCorrelationId()
        );
        response.setMessage(gatewayResponse.getMessage());
        response.setErrorMessage(payment.isFailed() ? gatewayResponse.getMessage() : null);
        return response;
    }

    private PaymentGatewayResponse gatewayFailureResponse(Exception exception) {
        PaymentGatewayResponse response = new PaymentGatewayResponse();
        response.setApproved(false);
        response.setStatus("FAILED");
        response.setMessage("Payment gateway error: " + exception.getMessage());
        response.setErrorCode("GATEWAY_ERROR");
        return response;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(MAX_PAYMENT_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                "Payment amount exceeds maximum limit of " + MAX_PAYMENT_AMOUNT
            );
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "FAILED";
        }
        return status.trim().toUpperCase();
    }

    private record ProcessedPayment(Payment payment, PaymentGatewayResponse gatewayResponse) {
    }
}

