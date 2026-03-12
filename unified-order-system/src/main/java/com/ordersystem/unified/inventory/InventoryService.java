package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.domain.InventoryBusinessRules;
import com.ordersystem.unified.inventory.dto.ReservationResponse;
import com.ordersystem.unified.inventory.dto.ReservationStatus;
import com.ordersystem.unified.inventory.model.Reservation;
import com.ordersystem.unified.inventory.model.ReservationItem;
import com.ordersystem.unified.inventory.model.Stock;
import com.ordersystem.unified.inventory.repository.ReservationItemRepository;
import com.ordersystem.unified.inventory.repository.ReservationRepository;
import com.ordersystem.unified.inventory.repository.StockRepository;
import com.ordersystem.unified.shared.events.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory service for stock management.
 *
 * Supports two modes controlled by {@code inventory.mock-mode}:
 * <ul>
 *   <li><b>mock-mode=false (default/production)</b>: full DB stock tracking with pessimistic
 *       locking; a Reservation entity plus ReservationItem rows are created for every
 *       reservation request.</li>
 *   <li><b>mock-mode=true (dev/test)</b>: creates only the Reservation row for audit trail;
 *       no Stock rows are touched.  {@code inventory.default-stock} controls the value
 *       returned by {@link #getAvailableQuantity}.</li>
 * </ul>
 *
 * When a product is not found in the Stock table the service always falls back to
 * {@code inventory.default-stock} for availability checks, so dev/test environments
 * that haven't seeded products still behave correctly.
 */
@Service
@Transactional
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationItemRepository reservationItemRepository;

    @Autowired
    private InventoryBusinessRules businessRules;

    /** When true, skip Stock DB mutations and only persist the Reservation for audit. */
    @Value("${inventory.mock-mode:false}")
    private boolean mockMode;

    /** Availability returned when a product has no Stock record in DB. */
    @Value("${inventory.default-stock:100}")
    private int defaultStock;

    // ---------------------------------------------------------------
    // Legacy Map-based API (backward compatibility with old callers)
    // ---------------------------------------------------------------

    public Map<String, Object> checkAvailability(String productId, int quantity) {
        int available = getAvailableQuantity(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("available", available >= quantity);
        result.put("currentStock", available);
        result.put("requestedQuantity", quantity);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    public Map<String, Object> reserveItems(String orderId, Map<String, Object> items) {
        // Legacy overload – creates a mock reservation without item-level tracking.
        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("reservationId", reservationId);
        result.put("status", "RESERVED");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    // ---------------------------------------------------------------
    // DTO-based API (preferred – used by CreateOrderUseCase)
    // ---------------------------------------------------------------

    /**
     * Backward-compatible overload that generates a temporary order-id.
     * Prefer {@link #reserveItems(String, List)} for full traceability.
     */
    public ReservationResponse reserveItems(List<OrderItem> items) {
        return reserveItems("ORD-UNKNOWN-" + UUID.randomUUID().toString().substring(0, 8), items);
    }

    /**
     * Reserves the given items for the specified order.
     *
     * <ul>
     *   <li>In real mode: validates stock availability, acquires pessimistic locks on Stock
     *       rows, creates Reservation + ReservationItem entities, and updates Stock counters.</li>
     *   <li>In mock mode: persists only the Reservation row (for audit) and returns success.</li>
     * </ul>
     */
    public ReservationResponse reserveItems(String orderId, List<OrderItem> items) {
        logger.info("Reserving {} item(s) for order: {} [mockMode={}]", items.size(), orderId, mockMode);

        if (mockMode) {
            return createMockReservation(orderId, items);
        }

        // Pre-flight check (no locks yet)
        for (OrderItem item : items) {
            int available = getAvailableQuantity(item.getProductId());
            if (item.getQuantity() > available) {
                logger.warn("Insufficient stock: product={}, available={}, requested={}",
                        item.getProductId(), available, item.getQuantity());
                return insufficientStockResponse(item.getProductId());
            }
        }

        return createRealReservation(orderId, items);
    }

    /**
     * Releases the reservation identified by {@code reservationId} and restores stock.
     */
    public Map<String, Object> releaseReservation(String reservationId) {
        doReleaseReservation(reservationId);
        Map<String, Object> result = new HashMap<>();
        result.put("reservationId", reservationId);
        result.put("status", "RELEASED");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    public void releaseItems(List<OrderItem> items, String reservationId) {
        logger.info("Releasing reservation: {}", reservationId);
        doReleaseReservation(reservationId);
    }

    public void releaseItems(List<OrderItem> items) {
        // No reservationId available – no-op; use releaseItems(items, reservationId) instead.
        logger.debug("releaseItems called without reservationId – skipped");
    }

    /**
     * Confirms the reservation, permanently deducting the reserved quantities from Stock.
     */
    public void confirmReservation(List<OrderItem> items, String reservationId) {
        logger.info("Confirming reservation: {}", reservationId);
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            reservation.markAsConfirmed();
            if (!mockMode) {
                List<ReservationItem> reservationItems =
                        reservationItemRepository.findByReservationId(reservationId);
                for (ReservationItem ri : reservationItems) {
                    if (ri.getStock() != null) {
                        ri.getStock().confirmReservation(ri.getReservedQuantity());
                        stockRepository.save(ri.getStock());
                    }
                }
            }
            reservationRepository.save(reservation);
            logger.debug("Reservation confirmed: {}", reservationId);
        });
    }

    // ---------------------------------------------------------------
    // Stock query helpers
    // ---------------------------------------------------------------

    /**
     * Returns total available quantity for a product across all warehouses.
     * Falls back to {@code inventory.default-stock} when no Stock row exists.
     */
    public Integer getAvailableQuantity(String productId) {
        Integer total = stockRepository.getTotalAvailableQuantityByProductId(productId);
        if (total != null && total > 0) {
            return total;
        }
        return defaultStock; // fallback for dev/test environments without seeded products
    }

    public Map<String, Object> getInventory(String productId) {
        int available = getAvailableQuantity(productId);
        Integer reserved = stockRepository.getTotalReservedQuantityByProductId(productId);
        int res = (reserved != null) ? reserved : 0;
        Map<String, Object> inventory = new HashMap<>();
        inventory.put("productId", productId);
        inventory.put("quantity", available + res);
        inventory.put("reserved", res);
        inventory.put("available", available);
        return inventory;
    }

    public Boolean hasSufficientQuantity(String productId, Integer quantity) {
        return getAvailableQuantity(productId) >= quantity;
    }

    public List<String> getLowStockProducts() {
        return stockRepository.findLowStockItems().stream()
                .map(s -> s.getProduct() != null ? s.getProduct().getId() : "UNKNOWN")
                .collect(Collectors.toList());
    }

    public List<String> getOutOfStockProducts() {
        return stockRepository.findOutOfStockItems().stream()
                .map(s -> s.getProduct() != null ? s.getProduct().getId() : "UNKNOWN")
                .collect(Collectors.toList());
    }

    public void addOrUpdateProduct(String productId, String productName, Integer quantity) {
        stockRepository.findByProductIdAndWarehouseId(productId, "DEFAULT").ifPresent(stock -> {
            stock.addStock(quantity);
            stockRepository.save(stock);
        });
    }

    public Map<String, Object> getInventoryStatus() {
        long totalProducts = stockRepository.count();
        long lowStockItems = stockRepository.findLowStockItems().size();
        long outOfStockItems = stockRepository.findOutOfStockItems().size();
        Map<String, Object> status = new HashMap<>();
        status.put("totalProducts", totalProducts);
        status.put("lowStockItems", lowStockItems);
        status.put("outOfStockItems", outOfStockItems);
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private ReservationResponse createMockReservation(String orderId, List<OrderItem> items) {
        // Even in mock mode, validate against the configured default stock ceiling
        for (OrderItem item : items) {
            if (item.getQuantity() > defaultStock) {
                logger.warn("Mock-mode insufficient stock: product={}, requested={}, limit={}",
                        item.getProductId(), item.getQuantity(), defaultStock);
                return insufficientStockResponse(item.getProductId());
            }
        }

        // Persist a Reservation for audit trail even in mock mode
        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8);
        Reservation reservation = new Reservation(
                reservationId, orderId, businessRules.calculateExpiryTime());
        reservationRepository.save(reservation);

        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservationId);
        response.setStatus(ReservationStatus.RESERVED);
        logger.debug("Mock reservation created: {} for order: {}", reservationId, orderId);
        return response;
    }

    private ReservationResponse createRealReservation(String orderId, List<OrderItem> items) {
        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8);

        Reservation reservation = new Reservation(
                reservationId, orderId, businessRules.calculateExpiryTime());
        reservationRepository.save(reservation);

        for (OrderItem item : items) {
            Optional<Stock> stockOpt = stockRepository
                    .findByProductIdAndWarehouseIdWithLock(item.getProductId(), "DEFAULT");

            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                try {
                    stock.reserveStock(item.getQuantity());
                    stockRepository.save(stock);

                    ReservationItem ri = new ReservationItem(
                            reservation, stock.getProduct(), stock,
                            item.getQuantity(), item.getQuantity());
                    reservationItemRepository.save(ri);

                } catch (IllegalStateException e) {
                    logger.warn("Failed to reserve stock for product {}: {}", item.getProductId(), e.getMessage());
                    reservation.markAsCancelled();
                    reservationRepository.save(reservation);
                    return insufficientStockResponse(item.getProductId());
                }
            } else {
                // Product not seeded in DB – graceful skip (dev/test compatibility)
                logger.debug("No Stock row for product {} – skipping stock deduction", item.getProductId());
            }
        }

        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservationId);
        response.setStatus(ReservationStatus.RESERVED);
        logger.info("Real reservation created: {} for order: {}", reservationId, orderId);
        return response;
    }

    private void doReleaseReservation(String reservationId) {
        reservationRepository.findById(reservationId).ifPresentOrElse(reservation -> {
            if (!reservation.canBeReleased()) {
                logger.warn("Reservation {} cannot be released (status={})",
                        reservationId, reservation.getStatus());
                return;
            }
            if (!mockMode) {
                List<ReservationItem> items = reservationItemRepository.findByReservationId(reservationId);
                for (ReservationItem ri : items) {
                    if (ri.getStock() != null) {
                        try {
                            ri.getStock().releaseReservation(ri.getReservedQuantity());
                            stockRepository.save(ri.getStock());
                        } catch (IllegalStateException e) {
                            logger.error("Failed to release stock for reservation item {}: {}", ri.getId(), e.getMessage());
                        }
                    }
                }
            }
            reservation.markAsReleased();
            reservationRepository.save(reservation);
            logger.info("Reservation released: {}", reservationId);
        }, () -> logger.warn("Reservation not found for release: {}", reservationId));
    }

    private ReservationResponse insufficientStockResponse(String productId) {
        ReservationResponse response = new ReservationResponse();
        response.setStatus(ReservationStatus.INSUFFICIENT_STOCK);
        response.setMessage("Insufficient stock for product: " + productId);
        return response;
    }
}
