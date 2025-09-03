package com.ordersystem.unified.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketEventService {

    @Autowired
    private WebSocketController webSocketController;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Object> lastHealthData = new ConcurrentHashMap<>();
    private final Map<String, Object> lastMetricsData = new ConcurrentHashMap<>();

    public void startPeriodicUpdates() {
        // Send health updates every 10 seconds
        scheduler.scheduleAtFixedRate(this::sendHealthUpdate, 0, 10, TimeUnit.SECONDS);
        
        // Send metrics updates every 30 seconds
        scheduler.scheduleAtFixedRate(this::sendMetricsUpdate, 0, 30, TimeUnit.SECONDS);
    }

    private void sendHealthUpdate() {
        try {
            // Simulate health data - in real implementation, get from health service
            Map<String, Object> healthData = Map.of(
                "status", "UP",
                "services", Map.of(
                    "database", Map.of("status", "UP", "responseTime", "45ms"),
                    "payment", Map.of("status", "UP", "responseTime", "120ms"),
                    "inventory", Map.of("status", "UP", "responseTime", "80ms"),
                    "order", Map.of("status", "UP", "responseTime", "60ms")
                ),
                "uptime", "2d 14h 32m",
                "memoryUsage", "68%"
            );

            // Only send if data has changed
            if (!healthData.equals(lastHealthData)) {
                webSocketController.sendHealthUpdate(healthData);
                lastHealthData.clear();
                lastHealthData.putAll(healthData);
            }
        } catch (Exception e) {
            System.err.println("Error sending health update: " + e.getMessage());
        }
    }

    private void sendMetricsUpdate() {
        try {
            // Simulate metrics data - in real implementation, get from metrics service
            Map<String, Object> metricsData = Map.of(
                "totalOrders", 1247,
                "totalRevenue", 89450.75,
                "pendingOrders", 23,
                "successRate", 94.2,
                "ordersToday", 45,
                "revenueToday", 3250.50,
                "averageOrderValue", 71.85,
                "topProducts", java.util.List.of(
                    Map.of("name", "Product A", "sales", 156),
                    Map.of("name", "Product B", "sales", 134),
                    Map.of("name", "Product C", "sales", 98)
                )
            );

            // Only send if data has changed significantly
            if (hasSignificantChange(metricsData, lastMetricsData)) {
                webSocketController.sendMetricsUpdate(metricsData);
                lastMetricsData.clear();
                lastMetricsData.putAll(metricsData);
            }
        } catch (Exception e) {
            System.err.println("Error sending metrics update: " + e.getMessage());
        }
    }

    private boolean hasSignificantChange(Map<String, Object> newData, Map<String, Object> oldData) {
        if (oldData.isEmpty()) return true;
        
        // Check if key metrics have changed
        Object newOrders = newData.get("totalOrders");
        Object oldOrders = oldData.get("totalOrders");
        
        if (newOrders != null && oldOrders != null) {
            return !newOrders.equals(oldOrders);
        }
        
        return true;
    }

    public void notifyOrderCreated(String orderId, Object orderData) {
        webSocketController.sendOrderUpdate(orderId, "CREATED", orderData);
        webSocketController.sendSystemUpdate("order_created", Map.of(
            "orderId", orderId,
            "message", "New order created: #" + orderId
        ));
    }

    public void notifyOrderUpdated(String orderId, String newStatus, Object orderData) {
        webSocketController.sendOrderUpdate(orderId, newStatus, orderData);
        webSocketController.sendSystemUpdate("order_updated", Map.of(
            "orderId", orderId,
            "status", newStatus,
            "message", "Order #" + orderId + " status changed to " + newStatus
        ));
    }

    public void notifyPaymentProcessed(String orderId, String paymentStatus, Object paymentData) {
        webSocketController.sendSystemUpdate("payment_processed", Map.of(
            "orderId", orderId,
            "status", paymentStatus,
            "data", paymentData,
            "message", "Payment " + paymentStatus.toLowerCase() + " for order #" + orderId
        ));
    }

    public void notifyInventoryUpdate(String productId, int newStock) {
        webSocketController.sendSystemUpdate("inventory_updated", Map.of(
            "productId", productId,
            "stock", newStock,
            "message", "Stock updated for product " + productId + ": " + newStock + " units"
        ));
    }

    public void notifySystemAlert(String alertType, String message, String severity) {
        webSocketController.sendSystemUpdate("system_alert", Map.of(
            "alertType", alertType,
            "message", message,
            "severity", severity,
            "requiresAction", "HIGH".equals(severity) || "CRITICAL".equals(severity)
        ));
    }

    public void sendUserNotification(String userId, String message, String type) {
        webSocketController.sendUserNotification(userId, message, type);
    }

    // Event listeners for application events
    @EventListener
    public void handleOrderEvent(Object event) {
        // Handle order-related events
        // This would be triggered by order service events
    }

    @EventListener
    public void handlePaymentEvent(Object event) {
        // Handle payment-related events
        // This would be triggered by payment service events
    }

    @EventListener
    public void handleInventoryEvent(Object event) {
        // Handle inventory-related events
        // This would be triggered by inventory service events
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}