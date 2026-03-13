package com.ordersystem.unified.resilience;

import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Resilience tests for the order system.
 * Tests system behavior under failure conditions.
 *
 * Critical scenarios tested:
 * - Database connection failures
 * - Timeout handling
 * - Recovery from failures
 * - Graceful degradation
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.ordersystem.unified.config.TestConfig.class)
public class ResilienceTest extends PostgresIntegrationTestSupport {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Test system recovery after temporary database failure")
    public void testDatabaseFailureRecovery() {
        // Given: System is healthy
        CreateOrderRequest request = createValidOrderRequest("DB-RECOVERY-TEST");

        // When: Create order successfully before failure
        OrderResponse beforeFailure = orderService.createOrder(request);
        assertNotNull(beforeFailure);

        // Simulate temporary database issue (query timeout)
        try {
            jdbcTemplate.setQueryTimeout(1); // 1 second timeout

            // Try to create order during "degraded" state
            CreateOrderRequest degradedRequest = createValidOrderRequest("DEGRADED-TEST");

            // Should handle gracefully (may fail or succeed depending on operation)
            try {
                orderService.createOrder(degradedRequest);
            } catch (Exception e) {
                // Expected during degraded state
                assertTrue(e.getMessage() != null);
            }

            // Reset timeout
            jdbcTemplate.setQueryTimeout(30);

            // Then: System should recover
            await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    CreateOrderRequest afterRecovery = createValidOrderRequest("RECOVERY-TEST");
                    OrderResponse response = orderService.createOrder(afterRecovery);
                    assertNotNull(response);
                });

        } finally {
            jdbcTemplate.setQueryTimeout(30); // Reset
        }
    }

    @Test
    @DisplayName("Test connection pool exhaustion handling")
    public void testConnectionPoolExhaustion() throws InterruptedException {
        // Given: Multiple concurrent requests that hold connections
        int numberOfThreads = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: Flood the system with requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();

                    CreateOrderRequest request = createValidOrderRequest("POOL-TEST-" + index);
                    OrderResponse response = orderService.createOrder(request);

                    if (response != null) {
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);

        // Then: System should handle gracefully
        assertTrue(completed, "Not all requests completed");

        System.out.println("Connection pool test - Success: " + successCount.get() +
                         ", Failures: " + failureCount.get());

        // Either succeed or fail gracefully (no deadlocks)
        assertTrue(successCount.get() + failureCount.get() == numberOfThreads,
            "Some requests were lost");

        // Most requests should succeed
        assertTrue(successCount.get() > numberOfThreads * 0.7,
            "Too many failures - connection pool may be too small");
    }

    @Test
    @DisplayName("Test graceful degradation with invalid data")
    public void testGracefulDegradationWithInvalidData() {
        // Given: Various invalid requests

        // Test 1: Empty customer ID
        CreateOrderRequest emptyCustomer = createValidOrderRequest("");
        assertThrows(Exception.class, () -> orderService.createOrder(emptyCustomer),
            "Should reject empty customer ID");

        // Test 2: Null items
        CreateOrderRequest nullItems = createValidOrderRequest("NULL-ITEMS");
        nullItems.setItems(null);
        assertThrows(Exception.class, () -> orderService.createOrder(nullItems),
            "Should reject null items");

        // Test 3: Empty items list
        CreateOrderRequest emptyItems = createValidOrderRequest("EMPTY-ITEMS");
        emptyItems.setItems(List.of());
        assertThrows(Exception.class, () -> orderService.createOrder(emptyItems),
            "Should reject empty items list");

        // Test 4: Negative quantity
        CreateOrderRequest negativeQty = createValidOrderRequest("NEGATIVE-QTY");
        OrderItemRequest item = negativeQty.getItems().get(0);
        item.setQuantity(-5);
        assertThrows(Exception.class, () -> orderService.createOrder(negativeQty),
            "Should reject negative quantity");

        // Test 5: Null price
        CreateOrderRequest nullPrice = createValidOrderRequest("NULL-PRICE");
        nullPrice.getItems().get(0).setUnitPrice(null);
        assertThrows(Exception.class, () -> orderService.createOrder(nullPrice),
            "Should reject null price");

        // Then: System should still be functional after invalid requests
        CreateOrderRequest validRequest = createValidOrderRequest("AFTER-INVALID");
        OrderResponse response = orderService.createOrder(validRequest);
        assertNotNull(response, "System should still work after handling invalid requests");
    }

    @Test
    @DisplayName("Test system behavior under extreme load with failures")
    public void testExtremeLoadWithFailures() throws InterruptedException {
        // Given: Mix of valid and invalid requests under high load
        int numberOfThreads = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger expectedFailureCount = new AtomicInteger(0);
        AtomicInteger unexpectedFailureCount = new AtomicInteger(0);

        // When: Send mixed requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();

                    CreateOrderRequest request;

                    // 70% valid, 30% invalid
                    if (index % 10 < 7) {
                        request = createValidOrderRequest("LOAD-VALID-" + index);
                    } else {
                        request = createInvalidOrderRequest("LOAD-INVALID-" + index);
                    }

                    try {
                        OrderResponse response = orderService.createOrder(request);
                        if (response != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        expectedFailureCount.incrementAndGet();
                    } catch (Exception e) {
                        unexpectedFailureCount.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(45, TimeUnit.SECONDS);

        // Then: System should handle mixed load
        assertTrue(completed, "Not all requests completed");

        System.out.println("Extreme load test - Success: " + successCount.get() +
                         ", Expected failures: " + expectedFailureCount.get() +
                         ", Unexpected failures: " + unexpectedFailureCount.get());

        // Verify success rate for valid requests
        int expectedValid = (int) (numberOfThreads * 0.7);
        assertTrue(successCount.get() >= expectedValid * 0.8,
            "Success rate too low for valid requests");

        // Unexpected failures should be minimal
        assertTrue(unexpectedFailureCount.get() < numberOfThreads * 0.1,
            "Too many unexpected failures");
    }

    @Test
    @DisplayName("Test timeout handling")
    public void testTimeoutHandling() {
        // Given: Request that should complete within reasonable time
        CreateOrderRequest request = createValidOrderRequest("TIMEOUT-TEST");

        // When: Execute with timeout
        long startTime = System.currentTimeMillis();

        try {
            OrderResponse response = orderService.createOrder(request);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Should complete within acceptable time
            assertNotNull(response);
            assertTrue(duration < 5000, // 5 seconds max
                "Order creation took too long: " + duration + "ms");

        } catch (Exception e) {
            fail("Order creation should not timeout for simple requests: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test error handling consistency")
    public void testErrorHandlingConsistency() {
        // Given: Same invalid request repeated multiple times
        CreateOrderRequest invalidRequest = createInvalidOrderRequest("CONSISTENCY-TEST");

        // When: Send same invalid request multiple times
        Exception firstException = null;
        for (int i = 0; i < 5; i++) {
            try {
                orderService.createOrder(invalidRequest);
                fail("Should have thrown exception");
            } catch (Exception e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    // Then: Should throw consistent error type
                    assertEquals(firstException.getClass(), e.getClass(),
                        "Error handling should be consistent");
                }
            }
        }
    }

    @Test
    @DisplayName("Test database connection validation")
    public void testDatabaseConnectionValidation() throws SQLException {
        // Given: Database connection
        Connection connection = dataSource.getConnection();

        try {
            // When: Verify connection is valid
            assertTrue(connection.isValid(5),
                "Database connection should be valid");

            // Then: Should be able to perform operations
            CreateOrderRequest request = createValidOrderRequest("DB-VALIDATION");
            OrderResponse response = orderService.createOrder(request);
            assertNotNull(response);

        } finally {
            connection.close();
        }

        // Verify connection was returned to pool
        Connection newConnection = dataSource.getConnection();
        assertNotNull(newConnection);
        newConnection.close();
    }

    // Helper methods

    private CreateOrderRequest createValidOrderRequest(String customerId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCustomerName("Resilience Test Customer");
        request.setCustomerEmail(customerId.toLowerCase() + "@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PROD-" + System.currentTimeMillis());
        item.setProductName("Resilience Test Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(List.of(item));
        return request;
    }

    private CreateOrderRequest createInvalidOrderRequest(String customerId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(""); // Invalid: empty ID
        request.setCustomerName("Invalid Test");
        request.setCustomerEmail("invalid@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("INVALID");
        item.setProductName("Invalid Product");
        item.setQuantity(-1); // Invalid: negative quantity
        item.setUnitPrice(new BigDecimal("-10.00")); // Invalid: negative price

        request.setItems(List.of(item));
        return request;
    }
}
