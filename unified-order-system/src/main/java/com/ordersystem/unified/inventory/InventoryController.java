package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.model.Product;
import com.ordersystem.unified.inventory.model.Stock;
import com.ordersystem.unified.inventory.repository.ProductRepository;
import com.ordersystem.unified.inventory.repository.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for inventory operations.
 * Connected to real database via Product/Stock repositories.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public InventoryController(InventoryService inventoryService,
                               ProductRepository productRepository,
                               StockRepository stockRepository) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Object>> checkStock(@PathVariable String productId,
                                                         @RequestParam int quantity) {
        Map<String, Object> result = inventoryService.checkAvailability(productId, quantity);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveStock(@RequestParam String orderId,
                                                           @RequestBody Map<String, Object> items) {
        Map<String, Object> result = inventoryService.reserveItems(orderId, items);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/release/{reservationId}")
    public ResponseEntity<Map<String, Object>> releaseStock(@PathVariable String reservationId) {
        Map<String, Object> result = inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getInventoryStatus() {
        long totalProducts = productRepository.countByActiveTrue();
        List<Stock> lowStockItems = stockRepository.findLowStockItems();
        List<Stock> outOfStockItems = stockRepository.findOutOfStockItems();

        Map<String, Object> status = new HashMap<>();
        status.put("totalProducts", totalProducts);
        status.put("lowStockItems", lowStockItems.size());
        status.put("outOfStockItems", outOfStockItems.size());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        List<Map<String, Object>> result = products.stream()
            .map(this::mapProductToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Map<String, Object>>> searchProducts(@RequestParam("q") String query) {
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query);
        List<Map<String, Object>> result = products.stream()
            .map(this::mapProductToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllInventory() {
        List<Product> products = productRepository.findByActiveTrue();

        List<Map<String, Object>> inventory = products.stream().map(product -> {
            Integer available = stockRepository.getTotalAvailableQuantityByProductId(product.getId());
            Integer reserved = stockRepository.getTotalReservedQuantityByProductId(product.getId());
            int totalStock = available + reserved;

            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("name", product.getName());
            item.put("stock", totalStock);
            item.put("reserved", reserved);
            item.put("available", available);
            item.put("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
            item.put("status", available <= 0 ? "OUT_OF_STOCK" : available <= 10 ? "LOW_STOCK" : "IN_STOCK");
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
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "inventory-service");
        health.put("status", "UP");
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
        map.put("stock", available + reserved);
        map.put("available", available);
        map.put("reserved", reserved);
        return map;
    }
}
