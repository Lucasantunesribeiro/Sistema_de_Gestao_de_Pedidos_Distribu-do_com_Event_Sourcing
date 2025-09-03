package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.dto.*;
import com.ordersystem.unified.inventory.model.*;
import com.ordersystem.unified.inventory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete Inventory Service with real database operations and reservation management
 */
@Service
@Transactional
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private static final String DEFAULT_WAREHOUSE = "DEFAULT";
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private ReservationItemRepository reservationItemRepository;
    
    /**
     * Reserve items with atomic operations and comprehensive error handling
     */
    public ReservationResponse reserveItems(ReservationRequest request) {
        String reservationId = UUID.randomUUID().toString();
        logger.info("Processing reservation: reservationId={}, orderId={}, items={}, correlationId={}", 
                   reservationId, request.getOrderId(), request.getItems().size(), request.getCorrelationId());
        
        try {
            // 1. Validate request
            validateReservationRequest(request);
            
            // 2. Check for existing active reservation for this order
            if (hasActiveReservationForOrder(request.getOrderId())) {
                return ReservationResponse.failure(reservationId, request.getOrderId(), 
                    "Order already has an active reservation");
            }
            
            // 3. Create reservation entity
            LocalDateTime expiryTime = LocalDateTime.now().plus(request.getReservationTimeout());
            Reservation reservation = new Reservation(reservationId, request.getOrderId(), expiryTime);
            reservation.setWarehouseId(request.getWarehouseId() != null ? request.getWarehouseId() : DEFAULT_WAREHOUSE);
            reservation.setCorrelationId(request.getCorrelationId());
            
            // 4. Process each item reservation
            List<ItemReservationResult> itemResults = new ArrayList<>();
            List<ReservationItem> reservationItems = new ArrayList<>();
            boolean hasAnyReservation = false;
            boolean hasFullReservation = true;
            
            for (ItemReservation itemRequest : request.getItems()) {
                ItemReservationResult result = processItemReservation(reservation, itemRequest, reservationItems);
                itemResults.add(result);
                
                if (result.getReservedQuantity() > 0) {
                    hasAnyReservation = true;
                }
                if (!result.isFullReservation()) {
                    hasFullReservation = false;
                }
            }
            
            // 5. Determine reservation status and save
            ReservationStatus status;
            String message;
            
            if (!hasAnyReservation) {
                status = ReservationStatus.INSUFFICIENT_STOCK;
                message = "No items could be reserved due to insufficient stock";
            } else if (hasFullReservation) {
                status = ReservationStatus.RESERVED;
                message = "All items reserved successfully";
            } else {
                status = ReservationStatus.PARTIAL;
                message = "Some items reserved, others unavailable";
            }
            
            reservation.setStatus(status);
            reservation = reservationRepository.save(reservation);
            
            // 6. Save reservation items
            for (ReservationItem item : reservationItems) {
                item.setReservation(reservation);
                reservationItemRepository.save(item);
            }
            
            // 7. Create response
            ReservationResponse response;
            if (status == ReservationStatus.INSUFFICIENT_STOCK) {
                response = ReservationResponse.insufficientStock(reservationId, request.getOrderId(), itemResults);
            } else if (status == ReservationStatus.PARTIAL) {
                response = ReservationResponse.partialSuccess(reservationId, request.getOrderId(), itemResults, expiryTime);
            } else {
                response = ReservationResponse.success(reservationId, request.getOrderId(), itemResults, expiryTime);
            }
            
            response.setCorrelationId(request.getCorrelationId());
            
            logger.info("Reservation processed: reservationId={}, status={}, itemsReserved={}, correlationId={}", 
                       reservationId, status, hasAnyReservation, request.getCorrelationId());
            
            return response;
            
        } catch (InventoryException e) {
            logger.warn("Inventory validation failed: reservationId={}, error={}", reservationId, e.getMessage());
            return ReservationResponse.failure(reservationId, request.getOrderId(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing reservation: reservationId={}, correlationId={}", 
                        reservationId, request.getCorrelationId(), e);
            return ReservationResponse.failure(reservationId, request.getOrderId(), 
                "Reservation processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public InventoryResult reserveItems(String orderId, Map<String, Integer> items, String correlationId) {
        logger.info("Processing legacy reservation for order: {}, items: {}, correlationId: {}", 
                   orderId, items, correlationId);
        
        // Convert to new format
        ReservationRequest request = new ReservationRequest();
        request.setOrderId(orderId);
        request.setCorrelationId(correlationId);
        
        List<ItemReservation> itemReservations = items.entrySet().stream()
            .map(entry -> new ItemReservation(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        request.setItems(itemReservations);
        
        ReservationResponse response = reserveItems(request);
        
        if (response.hasAnyReservation()) {
            return InventoryResult.success(response.getReservationId(), response.getMessage());
        } else {
            return InventoryResult.failure(response.getMessage());
        }
    }
    
    /**
     * Release reservation and return stock to available inventory
     */
    public boolean releaseReservation(String reservationId, String correlationId) {
        logger.info("Releasing reservation: reservationId={}, correlationId={}", reservationId, correlationId);
        
        try {
            Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                logger.warn("Reservation not found: reservationId={}", reservationId);
                return false;
            }
            
            Reservation reservation = reservationOpt.get();
            
            if (!reservation.canBeReleased()) {
                logger.warn("Reservation cannot be released: reservationId={}, status={}", 
                           reservationId, reservation.getStatus());
                return false;
            }
            
            // Release all reservation items
            List<ReservationItem> items = reservationItemRepository.findByReservationId(reservationId);
            for (ReservationItem item : items) {
                if (item.getAvailableForRelease() > 0) {
                    Stock stock = item.getStock();
                    stock.releaseReservation(item.getAvailableForRelease());
                    stockRepository.save(stock);
                    
                    item.releaseQuantity(item.getAvailableForRelease());
                    reservationItemRepository.save(item);
                }
            }
            
            // Update reservation status
            reservation.markAsReleased();
            reservationRepository.save(reservation);
            
            logger.info("Reservation released successfully: reservationId={}, correlationId={}", 
                       reservationId, correlationId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error releasing reservation: reservationId={}, correlationId={}", 
                        reservationId, correlationId, e);
            return false;
        }
    }
    
    /**
     * Confirm reservation and commit stock
     */
    public boolean confirmReservation(String reservationId, String correlationId) {
        logger.info("Confirming reservation: reservationId={}, correlationId={}", reservationId, correlationId);
        
        try {
            Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                logger.warn("Reservation not found: reservationId={}", reservationId);
                return false;
            }
            
            Reservation reservation = reservationOpt.get();
            
            if (!reservation.canBeConfirmed()) {
                logger.warn("Reservation cannot be confirmed: reservationId={}, status={}, expired={}", 
                           reservationId, reservation.getStatus(), reservation.isExpired());
                return false;
            }
            
            // Confirm all reservation items
            List<ReservationItem> items = reservationItemRepository.findByReservationId(reservationId);
            for (ReservationItem item : items) {
                if (item.getAvailableForConfirmation() > 0) {
                    Stock stock = item.getStock();
                    stock.confirmReservation(item.getAvailableForConfirmation());
                    stockRepository.save(stock);
                    
                    item.confirmQuantity(item.getAvailableForConfirmation());
                    reservationItemRepository.save(item);
                }
            }
            
            // Update reservation status
            reservation.markAsConfirmed();
            reservationRepository.save(reservation);
            
            logger.info("Reservation confirmed successfully: reservationId={}, correlationId={}", 
                       reservationId, correlationId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error confirming reservation: reservationId={}, correlationId={}", 
                        reservationId, correlationId, e);
            return false;
        }
    }
    
    /**
     * Get reservation status
     */
    public Optional<ReservationResponse> getReservationStatus(String reservationId) {
        logger.debug("Getting reservation status: reservationId={}", reservationId);
        
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Reservation reservation = reservationOpt.get();
        List<ReservationItem> items = reservationItemRepository.findByReservationId(reservationId);
        
        List<ItemReservationResult> itemResults = items.stream()
            .map(this::convertToItemResult)
            .collect(Collectors.toList());
        
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservation.getId());
        response.setOrderId(reservation.getOrderId());
        response.setStatus(reservation.getStatus());
        response.setMessage(reservation.getStatus().getDescription());
        response.setItemResults(itemResults);
        response.setReservationExpiry(reservation.getExpiryTime());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setCorrelationId(reservation.getCorrelationId());
        
        return Optional.of(response);
    }
    
    /**
     * Get inventory status for all products
     */
    public Map<String, Object> getInventoryStatus() {
        logger.debug("Getting complete inventory status");
        
        Map<String, Object> status = new HashMap<>();
        
        // Get all products with their stock information
        List<Product> products = productRepository.findByActiveTrue();
        Map<String, Map<String, Object>> productStatus = new HashMap<>();
        
        for (Product product : products) {
            List<Stock> stocks = stockRepository.findByProductId(product.getId());
            
            int totalAvailable = stocks.stream().mapToInt(Stock::getAvailableQuantity).sum();
            int totalReserved = stocks.stream().mapToInt(Stock::getReservedQuantity).sum();
            int totalStock = stocks.stream().mapToInt(Stock::getTotalQuantity).sum();
            
            Map<String, Object> productInfo = new HashMap<>();
            productInfo.put("productId", product.getId());
            productInfo.put("name", product.getName());
            productInfo.put("sku", product.getSku());
            productInfo.put("totalAvailable", totalAvailable);
            productInfo.put("totalReserved", totalReserved);
            productInfo.put("totalStock", totalStock);
            productInfo.put("warehouses", stocks.stream().collect(Collectors.toMap(
                Stock::getWarehouseId,
                stock -> Map.of(
                    "available", stock.getAvailableQuantity(),
                    "reserved", stock.getReservedQuantity(),
                    "total", stock.getTotalQuantity()
                )
            )));
            
            productStatus.put(product.getId(), productInfo);
        }
        
        status.put("products", productStatus);
        status.put("totalProducts", products.size());
        status.put("timestamp", LocalDateTime.now());
        
        return status;
    }
    
    /**
     * Check if product is available in requested quantity
     */
    public boolean isAvailable(String productId, Integer quantity) {
        return isAvailable(productId, quantity, DEFAULT_WAREHOUSE);
    }
    
    /**
     * Check if product is available in specific warehouse
     */
    public boolean isAvailable(String productId, Integer quantity, String warehouseId) {
        Optional<Stock> stockOpt = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId);
        boolean available = stockOpt.map(stock -> stock.canReserve(quantity)).orElse(false);
        
        logger.debug("Checking availability: productId={}, quantity={}, warehouse={}, available={}", 
                    productId, quantity, warehouseId, available);
        
        return available;
    }
    
    /**
     * Add stock for a product
     */
    public void addStock(String productId, Integer quantity, String warehouseId) {
        logger.info("Adding stock: productId={}, quantity={}, warehouse={}", productId, quantity, warehouseId);
        
        Optional<Stock> stockOpt = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId);
        
        if (stockOpt.isPresent()) {
            Stock stock = stockOpt.get();
            stock.addStock(quantity);
            stockRepository.save(stock);
        } else {
            // Create new stock entry
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Stock newStock = new Stock(productOpt.get(), warehouseId, quantity);
                stockRepository.save(newStock);
            } else {
                throw new InventoryException("Product not found: " + productId);
            }
        }
        
        logger.info("Stock added successfully: productId={}, quantity={}, warehouse={}", 
                   productId, quantity, warehouseId);
    }
    
    /**
     * Scheduled task to clean up expired reservations
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredReservations() {
        try {
            List<Reservation> expiredReservations = reservationRepository.findExpiredActiveReservations(LocalDateTime.now());
            
            for (Reservation reservation : expiredReservations) {
                logger.info("Auto-releasing expired reservation: reservationId={}, orderId={}", 
                           reservation.getId(), reservation.getOrderId());
                
                releaseReservation(reservation.getId(), "AUTO-CLEANUP");
                reservation.markAsExpired();
                reservationRepository.save(reservation);
            }
            
            if (!expiredReservations.isEmpty()) {
                logger.info("Cleaned up {} expired reservations", expiredReservations.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during expired reservation cleanup", e);
        }
    }
    
    // Private helper methods
    
    private void validateReservationRequest(ReservationRequest request) {
        if (request.getItems().isEmpty()) {
            throw new InventoryException("At least one item is required for reservation");
        }
        
        for (ItemReservation item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new InventoryException("Item quantity must be greater than zero");
            }
        }
    }
    
    private boolean hasActiveReservationForOrder(String orderId) {
        return reservationRepository.hasActiveReservationForOrder(orderId, LocalDateTime.now());
    }
    
    private ItemReservationResult processItemReservation(Reservation reservation, ItemReservation itemRequest, 
                                                        List<ReservationItem> reservationItems) {
        String productId = itemRequest.getProductId();
        Integer requestedQuantity = itemRequest.getQuantity();
        String warehouseId = itemRequest.getWarehouseId() != null ? itemRequest.getWarehouseId() : 
                           (reservation.getWarehouseId() != null ? reservation.getWarehouseId() : DEFAULT_WAREHOUSE);
        
        try {
            // Find product
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return ItemReservationResult.failure(productId, requestedQuantity, "Product not found");
            }
            
            Product product = productOpt.get();
            
            // Find stock with lock for atomic operation
            Optional<Stock> stockOpt = stockRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId);
            if (stockOpt.isEmpty()) {
                return ItemReservationResult.failure(productId, requestedQuantity, "No stock found for product in warehouse");
            }
            
            Stock stock = stockOpt.get();
            
            // Determine how much can be reserved
            Integer availableQuantity = stock.getAvailableQuantity();
            Integer reservedQuantity = Math.min(requestedQuantity, availableQuantity);
            
            if (reservedQuantity > 0) {
                // Reserve the stock
                stock.reserveStock(reservedQuantity);
                stockRepository.save(stock);
                
                // Create reservation item
                ReservationItem reservationItem = new ReservationItem(reservation, product, stock, 
                                                                     requestedQuantity, reservedQuantity);
                reservationItems.add(reservationItem);
                
                if (reservedQuantity.equals(requestedQuantity)) {
                    return ItemReservationResult.success(productId, reservedQuantity, availableQuantity, warehouseId);
                } else {
                    return ItemReservationResult.partial(productId, requestedQuantity, reservedQuantity, 
                                                        availableQuantity, warehouseId);
                }
            } else {
                return ItemReservationResult.insufficientStock(productId, requestedQuantity, availableQuantity);
            }
            
        } catch (Exception e) {
            logger.error("Error processing item reservation: productId={}, quantity={}", productId, requestedQuantity, e);
            return ItemReservationResult.failure(productId, requestedQuantity, "Error processing reservation: " + e.getMessage());
        }
    }
    
    private ItemReservationResult convertToItemResult(ReservationItem item) {
        return new ItemReservationResult(
            item.getProduct().getId(),
            item.getRequestedQuantity(),
            item.getReservedQuantity(),
            item.isFullyReserved(),
            item.isFullyReserved() ? "Fully reserved" : 
                (item.isPartiallyReserved() ? "Partially reserved" : "Not reserved")
        );
    }
    
    // Exception class
    public static class InventoryException extends RuntimeException {
        public InventoryException(String message) {
            super(message);
        }
    }
}