package com.ordersystem.query.service;

import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CacheInvalidationServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache ordersCache;

    @Mock
    private Cache customerOrdersCache;

    @Mock
    private Cache singleOrderCache;

    @Mock
    private Cache statusOrdersCache;

    @Mock
    private Cache orderStatsCache;

    private CacheInvalidationService cacheInvalidationService;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("orders")).thenReturn(ordersCache);
        when(cacheManager.getCache("customer-orders")).thenReturn(customerOrdersCache);
        when(cacheManager.getCache("single-order")).thenReturn(singleOrderCache);
        when(cacheManager.getCache("status-orders")).thenReturn(statusOrdersCache);
        when(cacheManager.getCache("order-stats")).thenReturn(orderStatsCache);

        cacheInvalidationService = new CacheInvalidationService(cacheManager);
    }

    @Test
    void shouldInvalidateOrderCacheOnStatusUpdate() {
        // Given
        String orderId = "order-123";
        String customerId = "customer-456";
        String oldStatus = "PENDING";
        String newStatus = "CONFIRMED";
        
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            orderId, customerId, oldStatus, newStatus, LocalDateTime.now(), null, null
        );

        // When
        cacheInvalidationService.handleOrderStatusUpdated(event);

        // Then
        verify(singleOrderCache).evict(orderId);
        verify(customerOrdersCache).evict(customerId);
        verify(statusOrdersCache).evict(oldStatus);
        verify(statusOrdersCache).evict(newStatus);
        verify(ordersCache).clear(); // Clear all orders cache
        verify(orderStatsCache).clear(); // Clear stats cache
    }

    @Test
    void shouldInvalidateOrderCacheOnPaymentProcessed() {
        // Given
        String orderId = "order-123";
        String paymentId = "payment-789";
        String paymentStatus = "APPROVED";
        double amount = 100.0;
        
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            orderId, paymentId, paymentStatus, amount, LocalDateTime.now()
        );

        // When
        cacheInvalidationService.handlePaymentProcessed(event);

        // Then
        verify(singleOrderCache).evict(orderId);
        verify(customerOrdersCache).clear(); // Now clears all customer caches
        verify(statusOrdersCache).evict("PENDING"); // Old status
        verify(statusOrdersCache).evict("PAID"); // New status based on payment
        verify(ordersCache).clear();
        verify(orderStatsCache).clear();
    }

    @Test
    void shouldInvalidateSpecificCustomerAndStatusCombination() {
        // Given
        String orderId = "order-123";
        String customerId = "customer-456";
        String status = "CONFIRMED";
        
        // When
        cacheInvalidationService.invalidateCustomerAndStatusCache(customerId, status);

        // Then
        verify(customerOrdersCache).evict(customerId);
        verify(statusOrdersCache).evict(status);
    }

    @Test
    void shouldHandleNullCacheGracefully() {
        // Given
        when(cacheManager.getCache(anyString())).thenReturn(null);
        
        String orderId = "order-123";
        String customerId = "customer-456";
        String oldStatus = "PENDING";
        String newStatus = "CONFIRMED";
        
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            orderId, customerId, oldStatus, newStatus, LocalDateTime.now(), null, null
        );

        // When & Then - Should not throw exception
        cacheInvalidationService.handleOrderStatusUpdated(event);
        
        // No cache operations should be performed
        verify(singleOrderCache, never()).evict(any());
    }

    @Test
    void shouldInvalidateAllCachesWhenRequested() {
        // When
        cacheInvalidationService.invalidateAllCaches();

        // Then
        verify(ordersCache).clear();
        verify(customerOrdersCache).clear();
        verify(singleOrderCache).clear();
        verify(statusOrdersCache).clear();
        verify(orderStatsCache).clear();
    }

    @Test
    void shouldGetCacheNames() {
        // When
        Set<String> cacheNames = cacheInvalidationService.getCacheNames();

        // Then
        assert cacheNames.contains("orders");
        assert cacheNames.contains("customer-orders");
        assert cacheNames.contains("single-order");
        assert cacheNames.contains("status-orders");
        assert cacheNames.contains("order-stats");
    }
}