package com.ordersystem.unified.payment.domain;

import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.shared.exceptions.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for Payment domain.
 * Encapsulates payment validation and business logic.
 *
 * Production-ready implementation with comprehensive validation.
 */
@Component
public class PaymentBusinessRules {

    private static final Logger logger = LoggerFactory.getLogger(PaymentBusinessRules.class);

    // Payment limits by method
    public static final BigDecimal PIX_MAX_AMOUNT = new BigDecimal("100000.00");
    public static final BigDecimal CREDIT_CARD_MAX_AMOUNT = new BigDecimal("50000.00");
    public static final BigDecimal DEBIT_CARD_MAX_AMOUNT = new BigDecimal("10000.00");

    // Retry configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MS = 2000;

    /**
     * Validates payment request before processing.
     *
     * @param orderId Order identifier
     * @param amount Payment amount
     * @param paymentMethod Payment method
     * @throws PaymentProcessingException if validation fails
     */
    public void validatePaymentRequest(String orderId, BigDecimal amount, PaymentMethod paymentMethod) {
        logger.debug("Validating payment: orderId={}, amount={}, method={}",
                    orderId, amount, paymentMethod);

        List<String> violations = new ArrayList<>();

        // Validate order ID
        if (orderId == null || orderId.trim().isEmpty()) {
            violations.add("Order ID is required");
        }

        // Validate amount
        if (amount == null) {
            violations.add("Payment amount is required");
        } else if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("Payment amount must be positive");
        }

        // Validate payment method
        if (paymentMethod == null) {
            violations.add("Payment method is required");
        } else {
            // Validate amount limits per payment method
            validatePaymentMethodLimits(amount, paymentMethod, violations);
        }

        if (!violations.isEmpty()) {
            String errorMessage = String.join("; ", violations);
            logger.warn("Payment validation failed: {}", errorMessage);
            throw new PaymentProcessingException(errorMessage);
        }

        logger.debug("Payment validation passed");
    }

    /**
     * Validates payment amount limits for specific payment method.
     */
    private void validatePaymentMethodLimits(BigDecimal amount, PaymentMethod method, List<String> violations) {
        BigDecimal maxAmount = switch (method) {
            case PIX -> PIX_MAX_AMOUNT;
            case CREDIT_CARD -> CREDIT_CARD_MAX_AMOUNT;
            case DEBIT_CARD -> DEBIT_CARD_MAX_AMOUNT;
            default -> CREDIT_CARD_MAX_AMOUNT; // Default max amount for other methods
        };

        if (amount.compareTo(maxAmount) > 0) {
            violations.add(String.format(
                "Amount %s exceeds maximum for %s: %s",
                amount, method, maxAmount
            ));
        }
    }

    /**
     * Determines if a payment error is retryable.
     *
     * @param errorCode Error code from payment processor
     * @return true if payment should be retried
     */
    public boolean isRetryableError(String errorCode) {
        if (errorCode == null) {
            return false;
        }

        // Retryable errors: temporary network issues, timeouts, etc.
        return switch (errorCode) {
            case "TIMEOUT",
                 "NETWORK_ERROR",
                 "TEMPORARY_UNAVAILABLE",
                 "RATE_LIMIT_EXCEEDED",
                 "SERVICE_UNAVAILABLE" -> true;
            default -> false;
        };
    }

    /**
     * Determines if payment method supports refunds.
     *
     * @param paymentMethod Payment method
     * @return true if refunds are supported
     */
    public boolean supportsRefund(PaymentMethod paymentMethod) {
        // All current methods support refunds
        // In production, this might vary by method/provider
        return switch (paymentMethod) {
            case PIX, CREDIT_CARD, DEBIT_CARD -> true;
            default -> true; // All methods support refunds
        };
    }

    /**
     * Calculates refund amount based on business rules.
     * Can apply refund fees, partial refunds, etc.
     *
     * @param originalAmount Original payment amount
     * @param paymentMethod Payment method
     * @return Refund amount
     */
    public BigDecimal calculateRefundAmount(BigDecimal originalAmount, PaymentMethod paymentMethod) {
        logger.debug("Calculating refund amount: original={}, method={}",
                    originalAmount, paymentMethod);

        // For PIX, apply 1% refund fee (business rule example)
        if (paymentMethod == PaymentMethod.PIX) {
            BigDecimal fee = originalAmount.multiply(new BigDecimal("0.01"));
            BigDecimal refundAmount = originalAmount.subtract(fee);
            logger.debug("PIX refund fee applied: {} (1%)", fee);
            return refundAmount;
        }

        // For cards, full refund
        return originalAmount;
    }

    /**
     * Determines processing priority based on payment method and amount.
     *
     * @param amount Payment amount
     * @param paymentMethod Payment method
     * @return Priority (1=high, 2=medium, 3=low)
     */
    public int determineProcessingPriority(BigDecimal amount, PaymentMethod paymentMethod) {
        // High value payments get priority
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            return 1; // High priority
        }

        // PIX payments are processed immediately
        if (paymentMethod == PaymentMethod.PIX) {
            return 1; // High priority
        }

        return 2; // Medium priority
    }
}
