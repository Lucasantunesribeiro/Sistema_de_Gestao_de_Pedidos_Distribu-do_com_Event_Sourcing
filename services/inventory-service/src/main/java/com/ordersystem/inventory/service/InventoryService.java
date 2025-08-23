package com.ordersystem.inventory.service;

import com.ordersystem.inventory.model.InventoryItem;
import com.ordersystem.inventory.model.StockReservation;
import com.ordersystem.inventory.repository.InventoryRepository;
import com.ordersystem.inventory.repository.StockReservationRepository;
import com.ordersystem.shared.events.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main inventory service with complete Event Sourcing and CQRS implementation
 */
@Service
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private StockReservationRepository reservationRepository;
    
    @Autowired
    private StockAllocationService allocationService;
    
    @Autowired
    private InventoryEventPublisher eventPublisher;
    
    @Value("${inventory.reservation.timeout.minutes:15}")
    private int reservationTimeoutMinutes;
    
    /**
     * Reserve inventory for an order (Saga Pattern)
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackReserveInventory")
    @Retry(name = "inventory-service")
    public void reserveInventory(InventoryReservationCommand command) {
        logger.info("Processing inventory reservation for order: {}", command.getOrderId());
        
        try {
            // Use allocation service for atomic stock reservation
            StockAllocationService.AllocationResult result = allocationService.allocateStock(
                command.getItems(), 
                command.getOrderId(), 
                StockAllocationService.AllocationStrategy.FIFO
            );
            
            if (result.isSuccess()) {
                // Create reservation record
                String reservationId = UUID.randomUUID().toString();
                List<StockReservation.ReservedItem> reservedItems = command.getItems().stream()
                    .map(item -> new StockReservation.ReservedItem(
                        item.getProductId(), 
                        item.getProductName(), 
                        item.getQuantity()
                    ))
                    .collect(Collectors.toList());
                
                StockReservation reservation = new StockReservation(
                    reservationId,
                    command.getOrderId(),
                    command.getCustomerId(),
                    reservedItems,
                    reservationTimeoutMinutes
                );
                
                reservationRepository.save(reservation);
                
                // Publish success event
                eventPublisher.publishInventoryReserved(
                    command.getOrderId(),
                    command.getCustomerId(),
                    command.getItems(),
                    reservationId
                );
                
                logger.info("Successfully reserved inventory for order {} with reservation {}", 
                    command.getOrderId(), reservationId);
                
            } else {
                // Publish failure event
                String failureReason = String.join("; ", result.getFailures());
                eventPublisher.publishInventoryReservationFailed(
                    command.getOrderId(),
                    command.getCustomerId(),
                    command.getItems(),
                    failureReason
                );
                
                logger.warn("Failed to reserve inventory for order {}: {}", 
                    command.getOrderId(), failureReason);
            }
            
        } catch (Exception e) {
            logger.error("Error processing inventory reservation for order {}", command.getOrderId(), e);
            
            eventPublisher.publishInventoryReservationFailed(
                command.getOrderId(),
                command.getCustomerId(),
                command.getItems(),
                "Internal service error: " + e.getMessage()
            );
        }
    }
    
    /**
     * Confirm inventory reservation (final allocation)
     */
    public void confirmReservation(InventoryConfirmationCommand command) {
        logger.info("Confirming inventory reservation for order: {}", command.getOrderId());
        
        StockReservation reservation = reservationRepository.findByOrderId(command.getOrderId());
        
        if (reservation != null && reservation.getStatus() == StockReservation.ReservationStatus.PENDING) {
            // Convert reserved items to OrderItems for allocation service
            List<OrderItem> items = reservation.getReservedItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getProductName(), item.getQuantity()))
                .collect(Collectors.toList());
            
            // Confirm allocation
            allocationService.confirmStock(items, command.getOrderId());
            
            // Update reservation status
            reservation.confirm();
            reservationRepository.save(reservation);
            
            logger.info("Confirmed inventory reservation {} for order {}", 
                reservation.getReservationId(), command.getOrderId());
        } else {
            logger.warn("No valid reservation found for order {} to confirm", command.getOrderId());
        }
    }
    
    /**
     * Release inventory reservation (compensation action)
     */
    public void releaseReservation(InventoryReleaseCommand command) {
        logger.info("Releasing inventory reservation for order: {}", command.getOrderId());
        
        StockReservation reservation = reservationRepository.findByOrderId(command.getOrderId());
        
        if (reservation != null) {
            // Convert reserved items to OrderItems for allocation service
            List<OrderItem> items = reservation.getReservedItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getProductName(), item.getQuantity()))
                .collect(Collectors.toList());
            
            // Release allocation
            allocationService.releaseStock(items, command.getOrderId(), command.getReason());
            
            // Update reservation status
            reservation.release();
            reservationRepository.save(reservation);
            
            logger.info("Released inventory reservation {} for order {} - reason: {}", 
                reservation.getReservationId(), command.getOrderId(), command.getReason());
        } else {
            logger.warn("No reservation found for order {} to release", command.getOrderId());
        }
    }
    
    /**
     * Get inventory item details
     */
    public InventoryItem getInventoryItem(String productId) {
        return inventoryRepository.findByProductId(productId);
    }
    
    /**
     * Get all inventory items
     */
    public Collection<InventoryItem> getAllInventoryItems() {
        return inventoryRepository.findAll();
    }
    
    /**
     * Check stock availability without reservation
     */
    public boolean checkStockAvailability(List<OrderItem> items) {
        return allocationService.checkAvailability(items);
    }
    
    /**
     * Get reservation by order ID
     */
    public StockReservation getReservationByOrderId(String orderId) {
        return reservationRepository.findByOrderId(orderId);
    }
    
    /**
     * Scheduled task to clean up expired reservations
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredReservations() {
        List<StockReservation> expiredReservations = reservationRepository.findExpiredReservations();
        
        for (StockReservation reservation : expiredReservations) {
            logger.info("Cleaning up expired reservation {} for order {}", 
                reservation.getReservationId(), reservation.getOrderId());
            
            // Convert reserved items to OrderItems for release
            List<OrderItem> items = reservation.getReservedItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getProductName(), item.getQuantity()))
                .collect(Collectors.toList());
            
            // Release the stock
            allocationService.releaseStock(items, reservation.getOrderId(), "Reservation expired");
            
            // Mark as expired and remove
            reservation.expire();
            reservationRepository.delete(reservation.getReservationId());
        }
        
        if (!expiredReservations.isEmpty()) {
            logger.info("Cleaned up {} expired reservations", expiredReservations.size());
        }
    }
    
    /**
     * Restock inventory
     */
    public void restockInventory(String productId, int quantity, String reason) {
        InventoryItem item = inventoryRepository.findByProductId(productId);
        
        if (item != null) {
            int previousQuantity = item.getAvailableQuantity();
            allocationService.restock(productId, quantity, reason);
            
            // Publish inventory updated event
            eventPublisher.publishInventoryUpdated(
                productId, 
                previousQuantity, 
                item.getAvailableQuantity(), 
                reason, 
                InventoryStatus.AVAILABLE
            );
            
            logger.info("Restocked {} units of product {} - reason: {}", quantity, productId, reason);
        }
    }
    
    // Fallback method for circuit breaker
    public void fallbackReserveInventory(InventoryReservationCommand command, Exception ex) {
        logger.error("Fallback: Failed to reserve inventory for order {} - service unavailable", 
            command.getOrderId(), ex);
        
        eventPublisher.publishInventoryReservationFailed(
            command.getOrderId(),
            command.getCustomerId(),
            command.getItems(),
            "Service temporarily unavailable - please try again later"
        );
    }
    
    /**
     * Get inventory statistics
     */
    public InventoryStats getInventoryStats() {
        long totalProducts = inventoryRepository.getTotalProducts();
        long totalAvailable = inventoryRepository.getTotalAvailableStock();
        long totalReserved = inventoryRepository.getTotalReservedStock();
        long pendingReservations = reservationRepository.countByStatus(StockReservation.ReservationStatus.PENDING);
        long expiredReservations = reservationRepository.countExpired();
        
        return new InventoryStats(totalProducts, totalAvailable, totalReserved, 
                                 pendingReservations, expiredReservations);
    }
    
    // Methods required by InventoryController
    
    /**
     * Get all items (alias for getAllInventoryItems)
     */
    public List<InventoryItem> getAllItems() {
        return (List<InventoryItem>) getAllInventoryItems();
    }
    
    /**
     * Get single item (alias for getInventoryItem)
     */
    public InventoryItem getItem(String productId) {
        return getInventoryItem(productId);
    }
    
    /**
     * Create new inventory item
     */
    public InventoryItem createItem(InventoryItem item) {
        inventoryRepository.save(item);
        return item;
    }
    
    /**
     * Update stock quantity
     */
    public InventoryItem updateStock(String productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId);
        if (item != null) {
            allocationService.restock(productId, quantity, "Manual stock update");
            return inventoryRepository.findByProductId(productId); // Return updated item
        }
        return null;
    }
    
    /**
     * Reserve stock for order
     */
    public boolean reserveStock(String productId, int quantity, String orderId) {
        try {
            OrderItem orderItem = new OrderItem(productId, "Product " + productId, quantity, java.math.BigDecimal.ZERO);
            List<OrderItem> items = List.of(orderItem);
            
            StockAllocationService.AllocationResult result = allocationService.allocateStock(
                items, orderId, StockAllocationService.AllocationStrategy.FIFO);
            
            if (result.isSuccess()) {
                // Create simple reservation record
                String reservationId = UUID.randomUUID().toString();
                List<StockReservation.ReservedItem> reservedItems = List.of(
                    new StockReservation.ReservedItem(productId, "Product " + productId, quantity)
                );
                
                StockReservation reservation = new StockReservation(
                    reservationId, orderId, "system", reservedItems, reservationTimeoutMinutes);
                reservationRepository.save(reservation);
                
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to reserve stock for product {} and order {}", productId, orderId, e);
            return false;
        }
    }
    
    /**
     * Release reserved stock
     */
    public void releaseStock(String productId, int quantity, String orderId) {
        try {
            OrderItem orderItem = new OrderItem(productId, "Product " + productId, quantity, java.math.BigDecimal.ZERO);
            List<OrderItem> items = List.of(orderItem);
            
            allocationService.releaseStock(items, orderId, "Manual release");
            
            // Remove reservation if exists
            StockReservation reservation = reservationRepository.findByOrderId(orderId);
            if (reservation != null) {
                reservation.release();
                reservationRepository.save(reservation);
            }
        } catch (Exception e) {
            logger.error("Failed to release stock for product {} and order {}", productId, orderId, e);
        }
    }

    public static class InventoryStats {
        private final long totalProducts;
        private final long totalAvailableStock;
        private final long totalReservedStock;
        private final long pendingReservations;
        private final long expiredReservations;
        
        public InventoryStats(long totalProducts, long totalAvailableStock, long totalReservedStock,
                             long pendingReservations, long expiredReservations) {
            this.totalProducts = totalProducts;
            this.totalAvailableStock = totalAvailableStock;
            this.totalReservedStock = totalReservedStock;
            this.pendingReservations = pendingReservations;
            this.expiredReservations = expiredReservations;
        }
        
        public long getTotalProducts() { return totalProducts; }
        public long getTotalAvailableStock() { return totalAvailableStock; }
        public long getTotalReservedStock() { return totalReservedStock; }
        public long getPendingReservations() { return pendingReservations; }
        public long getExpiredReservations() { return expiredReservations; }
    }
}