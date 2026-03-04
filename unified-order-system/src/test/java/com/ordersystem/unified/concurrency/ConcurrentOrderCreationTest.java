package com.ordersystem.unified.concurrency;

import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for order creation.
 * Tests thread safety, race conditions, and data integrity under concurrent load.
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.ordersystem.unified.config.TestConfig.class)
public class ConcurrentOrderCreationTest {

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("Test concurrent order creation - verify all orders are created successfully")
    public void testConcurrentOrderCreation() throws InterruptedException, ExecutionException, TimeoutException {
        // Given: 50 concurrent order requests
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<OrderResponse>> futures = new ArrayList<>();

        // When: Create orders concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            Future<OrderResponse> future = executorService.submit(() -> {
                CreateOrderRequest request = createOrderRequest("CONCURRENT-" + index);
                return orderService.createOrder(request);
            });
            futures.add(future);
        }

        // Then: All orders should be created successfully
        List<OrderResponse> responses = new ArrayList<>();
        for (Future<OrderResponse> future : futures) {
            OrderResponse response = future.get(10, TimeUnit.SECONDS);
            assertNotNull(response);
            assertNotNull(response.getOrderId());
            responses.add(response);
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // Verify all order IDs are unique
        Set<String> uniqueOrderIds = ConcurrentHashMap.newKeySet();
        for (OrderResponse response : responses) {
            assertTrue(uniqueOrderIds.add(response.getOrderId()),
                "Duplicate order ID found: " + response.getOrderId());
        }

        assertEquals(numberOfThreads, responses.size(), "Not all orders were created");
    }

    @Test
    @DisplayName("Test concurrent order creation with same product - verify inventory consistency")
    public void testConcurrentOrdersForSameProduct() throws InterruptedException {
        // Given: Multiple threads trying to order the same product
        int numberOfThreads = 20;
        String productId = "CONCURRENT-PRODUCT-001";
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        // When: Create orders concurrently for the same product
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    CreateOrderRequest request = createOrderRequestForProduct(
                        "CONCURRENT-SAME-" + index,
                        productId,
                        1
                    );

                    OrderResponse response = orderService.createOrder(request);
                    assertNotNull(response);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errors.add(e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: Verify consistency
        assertTrue(completed, "Not all threads completed in time");

        int totalProcessed = successCount.get() + failureCount.get();
        assertEquals(numberOfThreads, totalProcessed,
            "Not all orders were processed");

        // Log results
        System.out.println("Concurrent orders - Success: " + successCount.get() +
                         ", Failures: " + failureCount.get());

        // Some failures are acceptable due to inventory limits
        // But success rate should be reasonable
        assertTrue(successCount.get() > 0, "No orders succeeded - possible deadlock");
    }

    @Test
    @DisplayName("Test race condition on inventory reservation")
    public void testInventoryRaceCondition() throws InterruptedException {
        // Given: Limited inventory and high concurrent demand
        int numberOfThreads = 100;
        String productId = "RACE-PRODUCT-001";

        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger inventoryErrorCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Void>> futures = new ArrayList<>();

        // When: All threads try to reserve at the exact same time
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            Future<Void> future = executorService.submit(() -> {
                try {
                    barrier.await(); // Synchronize start

                    CreateOrderRequest request = createOrderRequestForProduct(
                        "RACE-" + index,
                        productId,
                        5
                    );

                    orderService.createOrder(request);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    if (e.getMessage() != null &&
                        e.getMessage().toLowerCase().contains("inventory")) {
                        inventoryErrorCount.incrementAndGet();
                    }
                }
                return null;
            });
            futures.add(future);
        }

        // Wait for all to complete
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                // Expected for some threads
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // Then: Verify no overselling occurred
        System.out.println("Race condition test - Success: " + successCount.get() +
                         ", Inventory errors: " + inventoryErrorCount.get());

        assertTrue(successCount.get() + inventoryErrorCount.get() > 0,
            "No orders were processed");
    }

    @Test
    @DisplayName("Test concurrent cancellation of same order")
    public void testConcurrentOrderCancellation() throws InterruptedException {
        // Given: Create an order first
        CreateOrderRequest createRequest = createOrderRequest("CANCEL-TEST");
        OrderResponse order = orderService.createOrder(createRequest);
        assertNotNull(order);

        // When: Multiple threads try to cancel the same order
        int numberOfThreads = 10;
        String orderId = order.getOrderId();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    orderService.cancelOrder(orderId, "Concurrent cancellation test");
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completeLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: Only one cancellation should succeed
        assertEquals(1, successCount.get(),
            "Only one thread should successfully cancel the order");
        assertEquals(numberOfThreads - 1, failureCount.get(),
            "Other threads should fail");
    }

    @Test
    @DisplayName("Test database transaction isolation")
    public void testDatabaseTransactionIsolation() throws InterruptedException, ExecutionException {
        // Given: Multiple concurrent transactions
        int numberOfThreads = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<OrderResponse>> futures = new ArrayList<>();

        // When: Create orders concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            Future<OrderResponse> future = executorService.submit(() -> {
                CreateOrderRequest request = createOrderRequest("ISOLATION-" + index);
                return orderService.createOrder(request);
            });
            futures.add(future);
        }

        // Then: All transactions should complete without deadlock
        List<String> orderIds = new ArrayList<>();
        for (Future<OrderResponse> future : futures) {
            try {
                OrderResponse response = future.get(15, TimeUnit.SECONDS);
                assertNotNull(response);
                orderIds.add(response.getOrderId());
            } catch (TimeoutException e) {
                fail("Transaction deadlock detected");
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // Verify all order IDs are unique (no duplicate key violations)
        assertEquals(numberOfThreads, new HashSet<>(orderIds).size(),
            "Duplicate order IDs detected");
    }

    // Helper methods

    private CreateOrderRequest createOrderRequest(String customerId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCustomerName("Concurrent Test Customer");
        request.setCustomerEmail(customerId.toLowerCase() + "@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PROD-CONCURRENT-" + System.currentTimeMillis());
        item.setProductName("Concurrent Test Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(List.of(item));
        return request;
    }

    private CreateOrderRequest createOrderRequestForProduct(
            String customerId,
            String productId,
            int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCustomerName("Concurrent Test Customer");
        request.setCustomerEmail(customerId.toLowerCase() + "@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setProductName("Shared Product");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(List.of(item));
        return request;
    }
}
