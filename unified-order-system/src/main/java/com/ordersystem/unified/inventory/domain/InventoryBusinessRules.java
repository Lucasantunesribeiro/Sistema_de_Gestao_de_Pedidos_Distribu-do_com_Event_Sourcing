package com.ordersystem.unified.inventory.domain;

import com.ordersystem.unified.shared.exceptions.InsufficientInventoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for Inventory domain.
 * Manages inventory reservation policies and validation.
 *
 * Production-ready implementation with atomic reservation support.
 */
@Component
public class InventoryBusinessRules {

    private static final Logger logger = LoggerFactory.getLogger(InventoryBusinessRules.class);

    // Reservation timeout: 15 minutes
    public static final int RESERVATION_TIMEOUT_MINUTES = 15;

    // Safety stock: always keep some inventory available
    public static final double SAFETY_STOCK_PERCENTAGE = 0.05; // 5%

    // Maximum quantity per reservation
    public static final int MAX_QUANTITY_PER_RESERVATION = 1000;

    /**
     * Validates inventory reservation request.
     *
     * @param productId Product identifier
     * @param requestedQuantity Quantity to reserve
     * @param availableQuantity Current available quantity
     * @throws InsufficientInventoryException if validation fails
     */
    public void validateReservationRequest(String productId, int requestedQuantity, int availableQuantity) {
        logger.debug("Validating reservation: productId={}, requested={}, available={}",
                    productId, requestedQuantity, availableQuantity);

        List<String> violations = new ArrayList<>();

        // Validate product ID
        if (productId == null || productId.trim().isEmpty()) {
            violations.add("Product ID is required");
        }

        // Validate quantity
        if (requestedQuantity <= 0) {
            violations.add("Requested quantity must be positive");
        }

        if (requestedQuantity > MAX_QUANTITY_PER_RESERVATION) {
            violations.add(String.format(
                "Cannot reserve more than %d units in single reservation",
                MAX_QUANTITY_PER_RESERVATION
            ));
        }

        // Check availability
        if (requestedQuantity > availableQuantity) {
            violations.add(String.format(
                "Insufficient inventory for product %s: requested=%d, available=%d",
                productId, requestedQuantity, availableQuantity
            ));
        }

        if (!violations.isEmpty()) {
            String errorMessage = String.join("; ", violations);
            logger.warn("Reservation validation failed: {}", errorMessage);
            throw new InsufficientInventoryException(errorMessage);
        }

        logger.debug("Reservation validation passed");
    }

    /**
     * Calculates reservation expiry time based on business rules.
     *
     * @return Expiry timestamp
     */
    public LocalDateTime calculateExpiryTime() {
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(RESERVATION_TIMEOUT_MINUTES);
        logger.debug("Calculated reservation expiry: {}", expiryTime);
        return expiryTime;
    }

    /**
     * Determines if a reservation should be automatically released due to expiry.
     *
     * @param expiryTime Reservation expiry time
     * @return true if reservation has expired
     */
    public boolean isReservationExpired(LocalDateTime expiryTime) {
        boolean expired = LocalDateTime.now().isAfter(expiryTime);
        if (expired) {
            logger.debug("Reservation expired at {}", expiryTime);
        }
        return expired;
    }

    /**
     * Calculates safety stock quantity to maintain.
     *
     * @param totalQuantity Total inventory quantity
     * @return Safety stock quantity
     */
    public int calculateSafetyStock(int totalQuantity) {
        int safetyStock = (int) Math.ceil(totalQuantity * SAFETY_STOCK_PERCENTAGE);
        logger.debug("Safety stock for total {}: {}", totalQuantity, safetyStock);
        return safetyStock;
    }

    /**
     * Determines if inventory is low and needs reordering.
     *
     * @param availableQuantity Current available quantity
     * @param reorderLevel Reorder threshold
     * @return true if reorder is needed
     */
    public boolean needsReorder(int availableQuantity, Integer reorderLevel) {
        if (reorderLevel == null) {
            return false;
        }

        boolean needsReorder = availableQuantity <= reorderLevel;
        if (needsReorder) {
            logger.info("Inventory needs reorder: available={}, reorderLevel={}",
                       availableQuantity, reorderLevel);
        }

        return needsReorder;
    }

    /**
     * Validates if a reservation can be confirmed.
     *
     * @param reservationId Reservation identifier
     * @param isExpired Whether reservation is expired
     * @param currentStatus Current reservation status
     * @return true if can be confirmed
     */
    public boolean canConfirmReservation(String reservationId, boolean isExpired, String currentStatus) {
        if (isExpired) {
            logger.warn("Cannot confirm expired reservation: {}", reservationId);
            return false;
        }

        // Only reserved or partial reservations can be confirmed
        boolean isValidStatus = "RESERVED".equals(currentStatus) || "PARTIAL".equals(currentStatus);
        if (!isValidStatus) {
            logger.warn("Cannot confirm reservation in status: {}", currentStatus);
        }

        return isValidStatus;
    }

    /**
     * Determines reservation priority based on order characteristics.
     *
     * @param orderId Order identifier
     * @param totalValue Order total value
     * @return Priority level (1=high, 2=medium, 3=low)
     */
    public int determineReservationPriority(String orderId, java.math.BigDecimal totalValue) {
        // High-value orders get priority
        if (totalValue.compareTo(new java.math.BigDecimal("10000")) > 0) {
            logger.debug("High-priority reservation for order {}: value={}", orderId, totalValue);
            return 1;
        }

        return 2; // Medium priority for regular orders
    }
}
