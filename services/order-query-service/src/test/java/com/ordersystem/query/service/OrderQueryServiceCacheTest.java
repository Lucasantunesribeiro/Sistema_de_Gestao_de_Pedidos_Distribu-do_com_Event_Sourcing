package com.ordersystem.query.service;

import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.repository.OrderReadModelRepository;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderQueryServiceCacheTest {

    @Autowired
    private OrderQueryService orderQueryService;

    @MockBean
    private OrderReadModelRepository orderReadModelRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    private OrderReadModel testOrder1;
    private OrderReadModel testOrder2;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheInvalidationService.invalidateAllCaches();

        testOrder1 = new OrderReadModel("order-1", "customer-1", "PENDING", 100.0, LocalDateTime.now());
        testOrder2 = new OrderReadModel("order-2", "customer-1", "CONFIRMED", 200.0, LocalDateTime.now());
    }

    @Test
    void shouldCacheGetAllOrders() {
        // Given
        List<OrderReadModel> orders = Arrays.asList(testOrder1, testOrder2);
        when(orderReadModelRepository.findAllOrderByCreatedAtDesc()).thenReturn(orders);

        // When - First call should hit database
        List<OrderReadModel> result1 = orderQueryService.getAllOrders();
        
        // When - Second call should hit cache
        List<OrderReadModel> result2 = orderQueryService.getAllOrders();

        // Then
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(2);
        
        // Repository should be called only once due to caching
        verify(orderReadModelRepository, times(1)).findAllOrderByCreatedAtDesc();
        
        // Cache should contain the result
        Cache ordersCache = cacheManager.getCache("orders");
        assertThat(ordersCache).isNotNull();
        assertThat(ordersCache.get("findAllOrderByCreatedAtDesc")).isNotNull();
    }

    @Test
    void shouldCacheGetOrdersByCustomerId() {
        // Given
        String customerId = "customer-1";
        List<OrderReadModel> orders = Arrays.asList(testOrder1, testOrder2);
        when(orderReadModelRepository.findByCustomerId(customerId)).thenReturn(orders);

        // When
        List<OrderReadModel> result1 = orderQueryService.getOrdersByCustomerId(customerId);
        List<OrderReadModel> result2 = orderQueryService.getOrdersByCustomerId(customerId);

        // Then
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(2);
        
        verify(orderReadModelRepository, times(1)).findByCustomerId(customerId);
        
        Cache customerOrdersCache = cacheManager.getCache("customer-orders");
        assertThat(customerOrdersCache.get(customerId)).isNotNull();
    }

    @Test
    void shouldCacheGetOrdersByStatus() {
        // Given
        String status = "PENDING";
        List<OrderReadModel> orders = Arrays.asList(testOrder1);
        when(orderReadModelRepository.findByStatus(status)).thenReturn(orders);

        // When
        List<OrderReadModel> result1 = orderQueryService.getOrdersByStatus(status);
        List<OrderReadModel> result2 = orderQueryService.getOrdersByStatus(status);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        
        verify(orderReadModelRepository, times(1)).findByStatus(status);
        
        Cache statusOrdersCache = cacheManager.getCache("status-orders");
        assertThat(statusOrdersCache.get(status)).isNotNull();
    }

    @Test
    void shouldCacheGetOrderById() {
        // Given
        String orderId = "order-1";
        when(orderReadModelRepository.findById(orderId)).thenReturn(Optional.of(testOrder1));

        // When
        Optional<OrderReadModel> result1 = orderQueryService.getOrderById(orderId);
        Optional<OrderReadModel> result2 = orderQueryService.getOrderById(orderId);

        // Then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        
        verify(orderReadModelRepository, times(1)).findById(orderId);
        
        Cache singleOrderCache = cacheManager.getCache("single-order");
        assertThat(singleOrderCache.get(orderId)).isNotNull();
    }

    @Test
    void shouldCacheGetOrdersByCustomerIdAndStatus() {
        // Given
        String customerId = "customer-1";
        String status = "PENDING";
        List<OrderReadModel> orders = Arrays.asList(testOrder1);
        when(orderReadModelRepository.findByCustomerIdAndStatus(customerId, status)).thenReturn(orders);

        // When
        List<OrderReadModel> result1 = orderQueryService.getOrdersByCustomerIdAndStatus(customerId, status);
        List<OrderReadModel> result2 = orderQueryService.getOrdersByCustomerIdAndStatus(customerId, status);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        
        verify(orderReadModelRepository, times(1)).findByCustomerIdAndStatus(customerId, status);
        
        Cache customerOrdersCache = cacheManager.getCache("customer-orders");
        String cacheKey = customerId + "::" + status;
        assertThat(customerOrdersCache.get(cacheKey)).isNotNull();
    }

    @Test
    void shouldInvalidateCacheOnOrderStatusUpdate() {
        // Given - Setup cached data
        String customerId = "customer-1";
        when(orderReadModelRepository.findByCustomerId(customerId)).thenReturn(Arrays.asList(testOrder1));
        orderQueryService.getOrdersByCustomerId(customerId); // Cache the result

        Cache customerOrdersCache = cacheManager.getCache("customer-orders");
        assertThat(customerOrdersCache.get(customerId)).isNotNull(); // Verify cached

        // When - Status update event
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            "order-1", customerId, "PENDING", "CONFIRMED", LocalDateTime.now(), null, null
        );
        cacheInvalidationService.handleOrderStatusUpdated(event);

        // Then - Cache should be invalidated
        assertThat(customerOrdersCache.get(customerId)).isNull();
    }

    @Test
    void shouldInvalidateCacheOnPaymentProcessed() {
        // Given - Setup cached data
        String orderId = "order-1";
        when(orderReadModelRepository.findById(orderId)).thenReturn(Optional.of(testOrder1));
        orderQueryService.getOrderById(orderId); // Cache the result

        Cache singleOrderCache = cacheManager.getCache("single-order");
        assertThat(singleOrderCache.get(orderId)).isNotNull(); // Verify cached

        // When - Payment processed event
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            orderId, "payment-123", "APPROVED", 100.0, LocalDateTime.now()
        );
        cacheInvalidationService.handlePaymentProcessed(event);

        // Then - Cache should be invalidated
        assertThat(singleOrderCache.get(orderId)).isNull();
    }

    @Test
    void shouldFallbackToDatabaseWhenCacheUnavailable() {
        // Given
        String customerId = "customer-1";
        List<OrderReadModel> orders = Arrays.asList(testOrder1);
        when(orderReadModelRepository.findByCustomerId(customerId)).thenReturn(orders);

        // When - Cache is cleared/unavailable
        cacheInvalidationService.invalidateAllCaches();
        List<OrderReadModel> result = orderQueryService.getOrdersByCustomerId(customerId);

        // Then - Should still return data from database
        assertThat(result).hasSize(1);
        verify(orderReadModelRepository, times(1)).findByCustomerId(customerId);
    }
}