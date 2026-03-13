package com.ordersystem.unified.order.domain;

import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for Order domain.
 * Encapsulates all business logic and validation rules for orders.
 *
 * Production-ready implementation following Domain-Driven Design principles.
 */
@Component
public class OrderBusinessRules {

    private static final Logger logger = LoggerFactory.getLogger(OrderBusinessRules.class);

    // Business constraints
    public static final BigDecimal MINIMUM_ORDER_VALUE = new BigDecimal("10.00");
    public static final BigDecimal MAXIMUM_ORDER_VALUE = new BigDecimal("10000000.00");
    public static final int MINIMUM_ITEMS = 1;
    public static final int MAXIMUM_ITEMS = 100;

    /**
     * Validates if an order can be created based on business rules.
     *
     * @param customerId Customer identifier
     * @param totalAmount Total order amount
     * @param itemCount Number of items
     * @throws InvalidOrderException if validation fails
     */
    public void validateOrderCreation(String customerId, BigDecimal totalAmount, int itemCount) {
        logger.debug("Validating order creation: customerId={}, totalAmount={}, itemCount={}",
                    customerId, totalAmount, itemCount);

        List<String> violations = new ArrayList<>();

        // Validate customer
        if (customerId == null || customerId.trim().isEmpty()) {
            violations.add("Customer ID is required");
        }

        // Validate total amount
        if (totalAmount == null) {
            violations.add("Total amount is required");
        } else {
            if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                violations.add("Total amount must be positive");
            }
            if (totalAmount.compareTo(MINIMUM_ORDER_VALUE) < 0) {
                violations.add(String.format("Order value must be at least %s", MINIMUM_ORDER_VALUE));
            }
            if (totalAmount.compareTo(MAXIMUM_ORDER_VALUE) > 0) {
                violations.add(String.format("Order value cannot exceed %s", MAXIMUM_ORDER_VALUE));
            }
        }

        // Validate item count
        if (itemCount < MINIMUM_ITEMS) {
            violations.add(String.format("Order must contain at least %d item(s)", MINIMUM_ITEMS));
        }
        if (itemCount > MAXIMUM_ITEMS) {
            violations.add(String.format("Order cannot contain more than %d items", MAXIMUM_ITEMS));
        }

        if (!violations.isEmpty()) {
            String errorMessage = String.join("; ", violations);
            logger.warn("Order validation failed: {}", errorMessage);
            throw new InvalidOrderException(errorMessage);
        }

        logger.debug("Order validation passed");
    }

    /**
     * Validates if an order can be cancelled based on current status.
     *
     * @param order Order to cancel
     * @throws InvalidOrderException if cancellation is not allowed
     */
    public void validateOrderCancellation(Order order) {
        logger.debug("Validating order cancellation: orderId={}, status={}",
                    order.getId(), order.getStatus());

        if (order.isTerminal()) {
            String message = String.format(
                "Cannot cancel order in terminal status: %s",
                order.getStatus()
            );
            logger.warn(message);
            throw new InvalidOrderException(message);
        }

        // Additional business rules for cancellation
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            // In production, you might want to allow cancellation with approval
            logger.info("Cancelling confirmed order {} - may require approval", order.getId());
        }

        logger.debug("Order cancellation validation passed");
    }

    /**
     * Validates if customer can place orders (not blocked).
     * In production, this would check against a customer service/database.
     *
     * @param customerId Customer identifier
     * @return true if customer can place orders
     */
    public boolean isCustomerAllowedToOrder(String customerId) {
        // TODO: Implement actual customer validation
        // Check if customer exists, is active, not blocked, etc.
        logger.debug("Validating customer eligibility: customerId={}", customerId);

        // Placeholder implementation - always returns true
        // In production: call customer service or query customer repository
        return true;
    }

    /**
     * Determines if an order status transition is valid.
     *
     * @param currentStatus Current order status
     * @param newStatus Desired new status
     * @return true if transition is valid
     */
    public boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        logger.debug("Validating status transition: {} -> {}", currentStatus, newStatus);

        // Terminal states cannot transition
        if (currentStatus.isTerminal()) {
            logger.warn("Cannot transition from terminal status: {}", currentStatus);
            return false;
        }

        // Define valid transitions
        return switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.INVENTORY_RESERVED ||
                          newStatus == OrderStatus.CANCELLED ||
                          newStatus == OrderStatus.FAILED;
            case INVENTORY_RESERVED -> newStatus == OrderStatus.PAYMENT_PROCESSING ||
                                     newStatus == OrderStatus.CANCELLED ||
                                     newStatus == OrderStatus.FAILED;
            case PAYMENT_PROCESSING -> newStatus == OrderStatus.CONFIRMED ||
                                      newStatus == OrderStatus.FAILED ||
                                      newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.CANCELLED;
            case CANCELLED, FAILED -> false; // Terminal states
        };
    }

    /**
     * Calculates risk score for an order (fraud detection placeholder).
     *
     * @param order Order to evaluate
     * @return Risk score (0-100, higher = more risky)
     */
    public int calculateRiskScore(Order order) {
        // TODO: Implement actual risk calculation
        // Consider: order value, customer history, payment method, etc.
        logger.debug("Calculating risk score for order: {}", order.getId());

        int score = 0;

        // High value orders are riskier
        if (order.getTotalAmount().compareTo(new BigDecimal("10000")) > 0) {
            score += 30;
        }

        // In production: check customer history, velocity, location, etc.

        logger.debug("Risk score for order {}: {}", order.getId(), score);
        return score;
    }

    /**
     * Determines if order requires manual approval based on business rules.
     *
     * @param order Order to evaluate
     * @return true if manual approval is required
     */
    public boolean requiresManualApproval(Order order) {
        int riskScore = calculateRiskScore(order);
        boolean highValue = order.getTotalAmount().compareTo(new BigDecimal("50000")) > 0;

        boolean requiresApproval = riskScore > 70 || highValue;

        if (requiresApproval) {
            logger.info("Order {} requires manual approval: riskScore={}, highValue={}",
                       order.getId(), riskScore, highValue);
        }

        return requiresApproval;
    }
}

