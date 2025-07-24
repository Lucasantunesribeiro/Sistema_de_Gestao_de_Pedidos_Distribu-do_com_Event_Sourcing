package com.ordersystem.order.service;

import com.ordersystem.order.dto.CreateOrderRequest;
import com.ordersystem.shared.events.OrderCreatedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Resilient Order Service with Circuit Breaker, Retry, and Timeout patterns
 */
@Service
public class ResilientOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientOrderService.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * Create order with resilience patterns
     */
    @CircuitBreaker(name = "payment-service", fallbackMethod = "createOrderFallback")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    @Bulkhead(name = "payment-service")
    public CompletableFuture<String> createOrderResilient(CreateOrderRequest request, String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("💼 Creating order with resilience patterns for customer: {}", customerId);
                
                // Delegate to original service
                String orderId = orderService.createOrder(request);
                
                // Simulate potential failure for testing
                if (Math.random() < 0.1) { // 10% failure rate for demo
                    throw new RuntimeException("Simulated service failure");
                }
                
                logger.info("✅ Order created successfully: {}", orderId);
                return orderId;
                
            } catch (Exception e) {
                logger.error("❌ Error creating order: {}", e.getMessage());
                throw new RuntimeException("Failed to create order", e);
            }
        });
    }
    
    /**
     * Fallback method for order creation
     */
    public CompletableFuture<String> createOrderFallback(CreateOrderRequest request, String customerId, Exception ex) {
        logger.warn("🔄 Circuit breaker activated - Using fallback for order creation. Reason: {}", ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            // Create a pending order that can be processed later
            String fallbackOrderId = "pending-" + System.currentTimeMillis();
            
            // Store for later processing (in real implementation, use a database)
            storePendingOrder(fallbackOrderId, request, customerId);
            
            logger.info("📋 Fallback order created: {} (will be processed when service recovers)", fallbackOrderId);
            return fallbackOrderId;
        });
    }
    
    /**
     * Process payment with resilience patterns
     */
    @CircuitBreaker(name = "payment-service", fallbackMethod = "processPaymentFallback")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    public CompletableFuture<Boolean> processPaymentResilient(String orderId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("💳 Processing payment with resilience patterns for order: {}", orderId);
                
                // Simulate payment processing
                Thread.sleep(1000); // Simulate processing time
                
                // Simulate potential failure
                if (Math.random() < 0.2) { // 20% failure rate for demo
                    throw new TimeoutException("Payment service timeout");
                }
                
                // Publish payment event
                publishPaymentEvent(orderId, amount);
                
                logger.info("✅ Payment processed successfully for order: {}", orderId);
                return true;
                
            } catch (Exception e) {
                logger.error("❌ Error processing payment: {}", e.getMessage());
                throw new RuntimeException("Payment processing failed", e);
            }
        });
    }
    
    /**
     * Fallback method for payment processing
     */
    public CompletableFuture<Boolean> processPaymentFallback(String orderId, double amount, Exception ex) {
        logger.warn("🔄 Payment circuit breaker activated - Using fallback. Reason: {}", ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            // Queue payment for later processing
            queuePaymentForRetry(orderId, amount);
            
            logger.info("📋 Payment queued for retry: {} (will be processed when service recovers)", orderId);
            return false; // Indicate that payment is pending
        });
    }
    
    /**
     * Check inventory with resilience patterns
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "checkInventoryFallback")
    @Retry(name = "inventory-service")
    @TimeLimiter(name = "inventory-service")
    public CompletableFuture<Boolean> checkInventoryResilient(String productId, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("📦 Checking inventory with resilience patterns for product: {}", productId);
                
                // Simulate inventory check
                Thread.sleep(500);
                
                // Simulate potential failure
                if (Math.random() < 0.15) { // 15% failure rate for demo
                    throw new RuntimeException("Inventory service unavailable");
                }
                
                // Simulate inventory availability (80% available)
                boolean available = Math.random() < 0.8;
                
                logger.info("📋 Inventory check result for {}: {}", productId, available ? "Available" : "Out of stock");
                return available;
                
            } catch (Exception e) {
                logger.error("❌ Error checking inventory: {}", e.getMessage());
                throw new RuntimeException("Inventory check failed", e);
            }
        });
    }
    
    /**
     * Fallback method for inventory check
     */
    public CompletableFuture<Boolean> checkInventoryFallback(String productId, int quantity, Exception ex) {
        logger.warn("🔄 Inventory circuit breaker activated - Using fallback. Reason: {}", ex.getMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            // Assume inventory is available (optimistic approach)
            // In production, you might check a cache or use historical data
            logger.info("📋 Fallback inventory check for {}: Assuming available (optimistic)", productId);
            return true;
        });
    }
    
    // Helper methods
    private void storePendingOrder(String orderId, CreateOrderRequest request, String customerId) {
        // In production, store in database with retry mechanism
        logger.info("💾 Storing pending order: {} for later processing", orderId);
    }
    
    private void queuePaymentForRetry(String orderId, double amount) {
        // In production, queue in reliable message queue
        logger.info("📤 Queuing payment for retry: {} - Amount: {}", orderId, amount);
    }
    
    private void publishPaymentEvent(String orderId, double amount) {
        try {
            // Publish payment processed event
            rabbitTemplate.convertAndSend("payment.fanout", "", 
                "Payment processed for order: " + orderId + ", amount: " + amount);
        } catch (Exception e) {
            logger.error("❌ Failed to publish payment event: {}", e.getMessage());
        }
    }
}