package com.ordersystem.unified.web;

import com.ordersystem.unified.inventory.repository.ProductRepository;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * REST controller for dashboard JSON API endpoints.
 * Reads real data from repositories.
 */
@RestController
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    public DashboardController(OrderRepository orderRepository,
                               PaymentRepository paymentRepository,
                               ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/api/dashboard")
    public Map<String, Object> getDashboard() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        BigDecimal totalRevenue = orderRepository.findByStatus(OrderStatus.CONFIRMED).stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("title", "Order Management Dashboard");
        dashboard.put("totalOrders", totalOrders);
        dashboard.put("pendingOrders", pendingOrders);
        dashboard.put("completedOrders", confirmedOrders);
        dashboard.put("cancelledOrders", cancelledOrders);
        dashboard.put("totalRevenue", totalRevenue);
        dashboard.put("timestamp", System.currentTimeMillis());
        return dashboard;
    }

    @GetMapping("/api/dashboard/recent-orders")
    public List<Map<String, Object>> getRecentOrders() {
        List<Order> recentOrders = orderRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 5,
                    org.springframework.data.domain.Sort.by("createdAt").descending())
        ).getContent();

        return recentOrders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.getId());
            map.put("status", order.getStatus().name());
            map.put("customerName", order.getCustomerName());
            map.put("totalAmount", order.getTotalAmount());
            map.put("createdAt", order.getCreatedAt());
            map.put("timestamp", System.currentTimeMillis());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/api/dashboard/health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");

        Map<String, Object> services = new HashMap<>();

        // Database check
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            orderRepository.count();
            dbHealth.put("status", "UP");
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            logger.error("Database health check failed", e);
        }
        services.put("database", dbHealth);

        // Payment service check
        Map<String, Object> paymentHealth = new HashMap<>();
        try {
            paymentRepository.count();
            paymentHealth.put("status", "UP");
        } catch (Exception e) {
            paymentHealth.put("status", "DOWN");
        }
        services.put("payment", paymentHealth);

        // Inventory service check
        Map<String, Object> inventoryHealth = new HashMap<>();
        try {
            productRepository.countByActiveTrue();
            inventoryHealth.put("status", "UP");
        } catch (Exception e) {
            inventoryHealth.put("status", "DOWN");
        }
        services.put("inventory", inventoryHealth);

        health.put("services", services);
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
