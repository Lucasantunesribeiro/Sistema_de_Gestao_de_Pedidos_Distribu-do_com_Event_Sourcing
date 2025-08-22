package com.ordersystem.query.service;

import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CacheInvalidationService {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Handles cache invalidation when order status is updated
     */
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        logger.info("Invalidating cache for order status update: orderId={}, oldStatus={}, newStatus={}", 
                   event.getOrderId(), event.getOldStatus(), event.getNewStatus());

        try {
            // Invalidate specific order cache
            evictFromCache("single-order", event.getOrderId());

            // Invalidate customer orders cache
            evictFromCache("customer-orders", event.getCustomerId());

            // Invalidate status-based caches for both old and new status
            evictFromCache("status-orders", event.getOldStatus());
            evictFromCache("status-orders", event.getNewStatus());

            // Clear general orders cache (affects getAllOrders)
            clearCache("orders");

            // Clear stats cache since order counts by status may have changed
            clearCache("order-stats");

            logger.info("Cache invalidation completed for order: {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error during cache invalidation for order: {}", event.getOrderId(), e);
            // Don't throw - cache invalidation failure shouldn't break the flow
        }
    }

    /**
     * Handles cache invalidation when payment is processed
     */
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        logger.info("Invalidating cache for payment processed: orderId={}, paymentStatus={}", 
                   event.getOrderId(), event.getPaymentStatus());

        try {
            // Invalidate specific order cache
            evictFromCache("single-order", event.getOrderId());

            // For payment events, we need to clear broader caches since we don't have customerId
            // This is less efficient but ensures consistency
            clearCache("customer-orders");

            // Invalidate status-based caches
            // Payment processing typically changes order status from PENDING to PAID/CANCELLED
            evictFromCache("status-orders", "PENDING");
            if ("APPROVED".equals(event.getPaymentStatus())) {
                evictFromCache("status-orders", "PAID");
            } else {
                evictFromCache("status-orders", "CANCELLED");
            }

            // Clear general orders cache
            clearCache("orders");

            // Clear stats cache
            clearCache("order-stats");

            logger.info("Cache invalidation completed for payment: {}", event.getPaymentId());

        } catch (Exception e) {
            logger.error("Error during cache invalidation for payment: {}", event.getPaymentId(), e);
        }
    }

    /**
     * Invalidates cache for specific customer and status combination
     */
    public void invalidateCustomerAndStatusCache(String customerId, String status) {
        logger.debug("Invalidating customer and status cache: customerId={}, status={}", customerId, status);
        
        evictFromCache("customer-orders", customerId);
        evictFromCache("status-orders", status);
    }

    /**
     * Clears all caches - use with caution
     */
    public void invalidateAllCaches() {
        logger.warn("Invalidating ALL caches - this may impact performance");
        
        getCacheNames().forEach(this::clearCache);
    }

    /**
     * Gets all cache names
     */
    public Set<String> getCacheNames() {
        return Set.of("orders", "customer-orders", "single-order", "status-orders", "order-stats");
    }

    /**
     * Safely evicts a key from cache
     */
    private void evictFromCache(String cacheName, String key) {
        if (key == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            logger.debug("Evicted key '{}' from cache '{}'", key, cacheName);
        } else {
            logger.warn("Cache '{}' not found for key eviction: {}", cacheName, key);
        }
    }

    /**
     * Safely clears entire cache
     */
    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            logger.debug("Cleared cache '{}'", cacheName);
        } else {
            logger.warn("Cache '{}' not found for clearing", cacheName);
        }
    }
}