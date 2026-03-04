package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.dto.*;
import com.ordersystem.unified.inventory.model.*;
import com.ordersystem.unified.inventory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for inventory operations.
 */
@RestController
@RequestMapping("/api/inventory")
@Transactional
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public InventoryController(InventoryService inventoryService,
                               ProductRepository productRepository,
                               StockRepository stockRepository,
                               ReservationRepository reservationRepository) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/check/{productId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> checkStock(@PathVariable String productId,
                                                         @RequestParam(defaultValue = "1") int quantity,
                                                         @RequestParam(required = false) String warehouseId) {
        List<Stock> stocks;
        if (warehouseId != null && !warehouseId.isEmpty()) {
            Optional<Stock> s = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId);
            stocks = s.map(List::of).orElse(Collections.emptyList());
        } else {
            stocks = stockRepository.findByProductId(productId);
        }

        int totalAvailable = stocks.stream().mapToInt(s -> s.getAvailableQuantity() != null ? s.getAvailableQuantity() : 0).sum();
        boolean available = totalAvailable >= quantity;

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("requestedQuantity", quantity);
        result.put("available", available);
        result.put("availableQuantity", totalAvailable);
        if (warehouseId != null) {
            result.put("warehouseId", warehouseId);
        }
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/stock/add")
    public ResponseEntity<Map<String, Object>> addStock(@RequestBody Map<String, Object> request) {
        String productId = (String) request.get("productId");
        int qty = ((Number) request.getOrDefault("quantity", 0)).intValue();
        String warehouseId = (String) request.getOrDefault("warehouseId", "DEFAULT");

        if (productId == null || productId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product ID is required"));
        }

        // Find or create product
        Product product = productRepository.findById(productId).orElseGet(() -> {
            Product p = new Product(productId, productId, productId, BigDecimal.ZERO);
            p.setActive(true);
            return productRepository.save(p);
        });

        // Find or create stock (single save to avoid double-flush issue with @CreationTimestamp)
        Stock stock = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
            .orElse(new Stock(product, warehouseId, 0));

        stock.addStock(qty);
        stock = stockRepository.save(stock);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Stock added successfully");
        result.put("productId", productId);
        result.put("warehouseId", warehouseId);
        result.put("quantityAdded", qty);
        result.put("totalAvailable", stock.getAvailableQuantity());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveItems(@RequestBody ReservationRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("code", "VALIDATION_ERROR", "message", "At least one item is required")
            );
        }

        String warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : "DEFAULT";
        List<ItemReservationResult> itemResults = new ArrayList<>();
        Map<String, Stock> stockMap = new java.util.LinkedHashMap<>();
        boolean allCanReserve = true;

        // First pass: check availability without modifying anything
        for (ItemReservation item : request.getItems()) {
            String productId = item.getProductId();
            int qty = item.getQuantity();

            Optional<Stock> stockOpt = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId);
            if (stockOpt.isEmpty()) {
                stockOpt = stockRepository.findByProductId(productId).stream().findFirst();
            }

            int available = stockOpt.map(Stock::getAvailableQuantity).orElse(0);
            if (stockOpt.isPresent() && stockOpt.get().canReserve(qty)) {
                stockMap.put(productId, stockOpt.get());
                itemResults.add(ItemReservationResult.success(productId, qty, available - qty, warehouseId));
            } else {
                itemResults.add(ItemReservationResult.insufficientStock(productId, qty, available));
                allCanReserve = false;
            }
        }

        if (!allCanReserve) {
            String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ReservationResponse failResponse = new ReservationResponse(
                reservationId, request.getOrderId(),
                ReservationStatus.INSUFFICIENT_STOCK, "insufficient stock for requested items"
            );
            failResponse.setItemResults(itemResults);
            return ResponseEntity.badRequest().body(failResponse);
        }

        // Second pass: actually reserve (all items can be reserved)
        for (ItemReservation item : request.getItems()) {
            stockMap.get(item.getProductId()).reserveStock(item.getQuantity());
        }
        stockRepository.saveAll(stockMap.values());

        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Duration timeout = request.getReservationTimeout() != null ? request.getReservationTimeout() : Duration.ofMinutes(15);
        LocalDateTime expiry = LocalDateTime.now().plus(timeout);

        Reservation reservation = new Reservation(reservationId, request.getOrderId(), expiry);
        reservation.setCorrelationId(request.getCorrelationId());
        reservation.setWarehouseId(warehouseId);
        reservationRepository.save(reservation);

        ReservationResponse response = ReservationResponse.success(reservationId, request.getOrderId(), itemResults, expiry);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservation/{reservationId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getReservation(@PathVariable String reservationId) {
        return reservationRepository.findById(reservationId)
            .map(reservation -> {
                ReservationResponse response = new ReservationResponse(
                    reservation.getId(), reservation.getOrderId(),
                    reservation.getStatus(), "Reservation found"
                );
                response.setReservationExpiry(reservation.getExpiryTime());
                response.setItemResults(Collections.emptyList());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/confirm/{reservationId}")
    public ResponseEntity<Map<String, Object>> confirmReservation(
            @PathVariable String reservationId,
            @RequestParam(required = false) String correlationId) {
        Optional<Reservation> opt = reservationRepository.findById(reservationId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Reservation reservation = opt.get();
        reservation.markAsConfirmed();
        reservationRepository.save(reservation);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Reservation confirmed successfully");
        result.put("reservationId", reservationId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/release/{reservationId}")
    public ResponseEntity<Map<String, Object>> releaseReservationPost(
            @PathVariable String reservationId,
            @RequestParam(required = false) String correlationId) {
        Optional<Reservation> opt = reservationRepository.findById(reservationId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Reservation reservation = opt.get();
        reservation.markAsReleased();
        reservationRepository.save(reservation);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Reservation released successfully");
        result.put("reservationId", reservationId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/release/{reservationId}")
    public ResponseEntity<Map<String, Object>> releaseReservation(@PathVariable String reservationId) {
        Optional<Reservation> opt = reservationRepository.findById(reservationId);
        Map<String, Object> result = new HashMap<>();
        if (opt.isPresent()) {
            opt.get().markAsReleased();
            reservationRepository.save(opt.get());
            result.put("success", true);
            result.put("message", "Reservation released");
            result.put("reservationId", reservationId);
        } else {
            result.put("success", true);
            result.put("message", "Reservation not found or already released");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reserve-legacy")
    public ResponseEntity<Map<String, Object>> reserveLegacy(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        String reservationId = "RES-LEGACY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reservationId", reservationId);
        result.put("orderId", orderId);
        result.put("status", "RESERVED");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getInventoryStatus() {
        long totalProducts = productRepository.countByActiveTrue();
        List<Stock> lowStockItems = stockRepository.findLowStockItems();
        List<Stock> outOfStockItems = stockRepository.findOutOfStockItems();

        Map<String, Object> status = new HashMap<>();
        status.put("products", totalProducts);
        status.put("totalProducts", totalProducts);
        status.put("lowStockItems", lowStockItems.size());
        status.put("outOfStockItems", outOfStockItems.size());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        List<Map<String, Object>> result = products.stream()
            .map(this::mapProductToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/search")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> searchProducts(@RequestParam("q") String query) {
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query);
        List<Map<String, Object>> result = products.stream()
            .map(this::mapProductToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllInventory() {
        List<Product> products = productRepository.findByActiveTrue();
        List<Map<String, Object>> inventory = products.stream().map(product -> {
            Integer available = stockRepository.getTotalAvailableQuantityByProductId(product.getId());
            Integer reserved = stockRepository.getTotalReservedQuantityByProductId(product.getId());
            int totalStock = (available != null ? available : 0) + (reserved != null ? reserved : 0);

            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("name", product.getName());
            item.put("stock", totalStock);
            item.put("reserved", reserved != null ? reserved : 0);
            item.put("available", available != null ? available : 0);
            item.put("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
            item.put("status", available == null || available <= 0 ? "OUT_OF_STOCK" : available <= 10 ? "LOW_STOCK" : "IN_STOCK");
            item.put("timestamp", System.currentTimeMillis());
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product name is required"));
        }

        String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sku = (String) request.getOrDefault("sku", productId);

        BigDecimal price = BigDecimal.ZERO;
        Object priceObj = request.get("price");
        if (priceObj instanceof Number) {
            price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
        }

        int quantity = 0;
        Object qtyObj = request.get("quantity");
        if (qtyObj instanceof Number) {
            quantity = ((Number) qtyObj).intValue();
        }

        Product product = new Product(productId, name.trim(), sku, price);
        product.setDescription((String) request.get("description"));
        product.setCategory((String) request.get("category"));
        productRepository.save(product);

        if (quantity > 0) {
            Stock stock = new Stock(product, "DEFAULT", quantity);
            stockRepository.save(stock);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("name", product.getName());
        response.put("sku", sku);
        response.put("price", price);
        response.put("quantity", quantity);
        response.put("status", "CREATED");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "inventory-service");
        health.put("status", "UP");
        health.put("version", "2.0");
        health.put("features", List.of("inventory-reservation", "stock-management", "multi-warehouse-support"));
        health.put("totalProducts", productRepository.countByActiveTrue());
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> mapProductToResponse(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("productId", product.getId());
        map.put("name", product.getName());
        map.put("description", product.getDescription());
        map.put("sku", product.getSku());
        map.put("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
        map.put("category", product.getCategory());

        Integer available = stockRepository.getTotalAvailableQuantityByProductId(product.getId());
        Integer reserved = stockRepository.getTotalReservedQuantityByProductId(product.getId());
        map.put("stock", (available != null ? available : 0) + (reserved != null ? reserved : 0));
        map.put("available", available != null ? available : 0);
        map.put("reserved", reserved != null ? reserved : 0);
        return map;
    }
}
