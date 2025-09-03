package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Complete Inventory Controller with full REST API
 */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Service", description = "Complete inventory management operations")
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    @Autowired
    private InventoryService inventoryService;
    
    public InventoryController() {
        logger.info("InventoryController initialized");
    }
    
    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory", description = "Reserve inventory items with full validation and error handling")
    public ResponseEntity<ReservationResponse> reserveInventory(
            @Valid @RequestBody @Parameter(description = "Reservation request") ReservationRequest request) {
        
        try {
            logger.info("Processing inventory reservation: orderId={}, items={}, correlationId={}", 
                       request.getOrderId(), request.getItems().size(), request.getCorrelationId());
            
            ReservationResponse response = inventoryService.reserveItems(request);
            
            if (response.hasAnyReservation()) {
                logger.info("Inventory reservation processed: reservationId={}, status={}", 
                           response.getReservationId(), response.getStatus());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Inventory reservation failed: reservationId={}, message={}", 
                           response.getReservationId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing inventory reservation: {}", e.getMessage(), e);
            
            ReservationResponse errorResponse = ReservationResponse.failure(
                "ERROR-" + System.currentTimeMillis(), 
                request.getOrderId(), 
                "Inventory reservation failed: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/reserve-legacy")
    @Operation(summary = "Reserve inventory (legacy)", description = "Legacy inventory reservation for backward compatibility")
    public ResponseEntity<Map<String, Object>> reserveInventoryLegacy(
            @RequestBody @Parameter(description = "Legacy reservation request") Map<String, Object> request) {
        
        try {
            String orderId = (String) request.get("orderId");
            @SuppressWarnings("unchecked")
            Map<String, Integer> items = (Map<String, Integer>) request.get("items");
            String correlationId = (String) request.get("correlationId");
            
            logger.info("Processing legacy inventory reservation: orderId={}, items={}", orderId, items);
            
            InventoryResult result = inventoryService.reserveItems(orderId, items, correlationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("reservationId", result.getReservationId());
            response.put("timestamp", System.currentTimeMillis());
            
            if (result.isSuccess()) {
                logger.info("Legacy inventory reserved successfully: orderId={}, reservationId={}", 
                           orderId, result.getReservationId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Legacy inventory reservation failed: orderId={}, message={}", orderId, result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing legacy inventory reservation: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Inventory reservation failed: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/release/{reservationId}")
    @Operation(summary = "Release reservation", description = "Release a previously made inventory reservation")
    public ResponseEntity<Map<String, Object>> releaseReservation(
            @PathVariable @Parameter(description = "Reservation ID") String reservationId,
            @RequestParam(required = false) @Parameter(description = "Correlation ID") String correlationId) {
        
        try {
            logger.info("Releasing reservation: reservationId={}, correlationId={}", reservationId, correlationId);
            
            boolean success = inventoryService.releaseReservation(reservationId, correlationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Reservation released successfully" : "Failed to release reservation");
            response.put("reservationId", reservationId);
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error releasing reservation: reservationId={}", reservationId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to release reservation: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/confirm/{reservationId}")
    @Operation(summary = "Confirm reservation", description = "Confirm a reservation and commit the stock")
    public ResponseEntity<Map<String, Object>> confirmReservation(
            @PathVariable @Parameter(description = "Reservation ID") String reservationId,
            @RequestParam(required = false) @Parameter(description = "Correlation ID") String correlationId) {
        
        try {
            logger.info("Confirming reservation: reservationId={}, correlationId={}", reservationId, correlationId);
            
            boolean success = inventoryService.confirmReservation(reservationId, correlationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Reservation confirmed successfully" : "Failed to confirm reservation");
            response.put("reservationId", reservationId);
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error confirming reservation: reservationId={}", reservationId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to confirm reservation: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Get reservation status", description = "Get the status of a specific reservation")
    public ResponseEntity<ReservationResponse> getReservationStatus(
            @PathVariable @Parameter(description = "Reservation ID") String reservationId) {
        
        try {
            logger.debug("Getting reservation status: reservationId={}", reservationId);
            
            Optional<ReservationResponse> response = inventoryService.getReservationStatus(reservationId);
            
            if (response.isPresent()) {
                return ResponseEntity.ok(response.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error getting reservation status: reservationId={}", reservationId, e);
            
            ReservationResponse errorResponse = ReservationResponse.failure(
                reservationId, null, "Failed to get reservation status: " + e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/status")
    @Operation(summary = "Get inventory status", description = "Get current inventory levels for all products")
    public ResponseEntity<Map<String, Object>> getInventoryStatus() {
        
        try {
            logger.debug("Getting complete inventory status");
            
            Map<String, Object> inventory = inventoryService.getInventoryStatus();
            return ResponseEntity.ok(inventory);
            
        } catch (Exception e) {
            logger.error("Error getting inventory status: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get inventory status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/check/{productId}")
    @Operation(summary = "Check product availability", description = "Check if a product is available in the requested quantity")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable @Parameter(description = "Product ID") String productId,
            @RequestParam @Parameter(description = "Required quantity") Integer quantity,
            @RequestParam(required = false) @Parameter(description = "Warehouse ID") String warehouseId) {
        
        try {
            logger.debug("Checking availability: productId={}, quantity={}, warehouse={}", 
                        productId, quantity, warehouseId);
            
            boolean available = warehouseId != null ? 
                inventoryService.isAvailable(productId, quantity, warehouseId) :
                inventoryService.isAvailable(productId, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("requestedQuantity", quantity);
            response.put("warehouseId", warehouseId);
            response.put("available", available);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking availability: productId={}", productId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check availability: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/stock/add")
    @Operation(summary = "Add stock", description = "Add stock for a product in a warehouse")
    public ResponseEntity<Map<String, Object>> addStock(
            @RequestBody @Parameter(description = "Stock addition request") Map<String, Object> request) {
        
        try {
            String productId = (String) request.get("productId");
            Integer quantity = (Integer) request.get("quantity");
            String warehouseId = (String) request.getOrDefault("warehouseId", "DEFAULT");
            
            logger.info("Adding stock: productId={}, quantity={}, warehouse={}", productId, quantity, warehouseId);
            
            inventoryService.addStock(productId, quantity, warehouseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock added successfully");
            response.put("productId", productId);
            response.put("quantity", quantity);
            response.put("warehouseId", warehouseId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error adding stock: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to add stock: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Inventory service health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "inventory-service");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "2.0");
        health.put("features", List.of(
            "inventory-reservation",
            "stock-management", 
            "reservation-expiry",
            "multi-warehouse-support",
            "atomic-operations"
        ));
        
        return ResponseEntity.ok(health);
    }
}