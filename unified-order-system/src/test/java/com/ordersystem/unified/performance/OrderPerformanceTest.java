package com.ordersystem.unified.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.OrderRequest;
import com.ordersystem.unified.shared.events.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the unified order system.
 * Tests system behavior under load and concurrent access.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class OrderPerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRequest sampleOrderRequest;

    @BeforeEach
    void setUp() {
        sampleOrderRequest = new OrderRequest();
        sampleOrderRequest.setCustomerId("perf-test-customer");
        
        OrderItem item = new OrderItem();
        item.setProductId("product-1");
        item.setQuantity(1);
        item.setPrice(BigDecimal.valueOf(99.99));
        
        sampleOrderRequest.setItems(Arrays.asList(item));
    }

    @Test
    void testOrderCreationPerformance() throws Exception {
        int numberOfOrders = 100;
        long startTime = System.currentTimeMillis();

        // Create multiple orders sequentially
        for (int i = 0; i < numberOfOrders; i++) {
            OrderRequest request = new OrderRequest();
            request.setCustomerId("perf-customer-" + i);
            
            OrderItem item = new OrderItem();
            item.setProductId("product-1");
            item.setQuantity(1);
            item.setPrice(BigDecimal.valueOf(99.99));
            
            request.setItems(Arrays.asList(item));

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageTime = (double) totalTime / numberOfOrders;

        System.out.println("Created " + numberOfOrders + " orders in " + totalTime + "ms");
        System.out.println("Average time per order: " + averageTime + "ms");

        // Assert reasonable performance (adjust threshold as needed)
        assertTrue(averageTime < 1000, "Average order creation time should be less than 1 second");
    }

    @Test
    void testConcurrentOrderCreation() throws Exception {
        int numberOfThreads = 10;
        int ordersPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.currentTimeMillis();

        // Create concurrent order creation tasks
        CompletableFuture<Void>[] futures = IntStream.range(0, numberOfThreads)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    try {
                        for (int i = 0; i < ordersPerThread; i++) {
                            OrderRequest request = new OrderRequest();
                            request.setCustomerId("concurrent-customer-" + threadId + "-" + i);
                            
                            OrderItem item = new OrderItem();
                            item.setProductId("product-1");
                            item.setQuantity(1);
                            item.setPrice(BigDecimal.valueOf(99.99));
                            
                            request.setItems(Arrays.asList(item));

                            mockMvc.perform(post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                                    .andExpect(status().isCreated());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create order", e);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int totalOrders = numberOfThreads * ordersPerThread;

        System.out.println("Created " + totalOrders + " orders concurrently in " + totalTime + "ms");
        System.out.println("Throughput: " + (totalOrders * 1000.0 / totalTime) + " orders/second");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testQueryPerformance() throws Exception {
        // First, create some test data
        int numberOfOrders = 50;
        for (int i = 0; i < numberOfOrders; i++) {
            OrderRequest request = new OrderRequest();
            request.setCustomerId("query-perf-customer");
            
            OrderItem item = new OrderItem();
            item.setProductId("product-" + (i % 5 + 1)); // Distribute across 5 products
            item.setQuantity(1);
            item.setPrice(BigDecimal.valueOf(99.99));
            
            request.setItems(Arrays.asList(item));

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Test query performance
        long startTime = System.currentTimeMillis();

        // Perform multiple queries
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/query/orders/customer/query-perf-customer"))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageQueryTime = (double) totalTime / 20;

        System.out.println("Executed 20 customer queries in " + totalTime + "ms");
        System.out.println("Average query time: " + averageQueryTime + "ms");

        // Assert reasonable query performance
        assertTrue(averageQueryTime < 500, "Average query time should be less than 500ms");
    }

    @Test
    void testInventoryReservationUnderLoad() throws Exception {
        int numberOfConcurrentOrders = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentOrders);

        // All orders will try to reserve from the same product
        String productId = "product-1";
        int quantityPerOrder = 1;

        CompletableFuture<Boolean>[] futures = IntStream.range(0, numberOfConcurrentOrders)
                .mapToObj(orderId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        OrderRequest request = new OrderRequest();
                        request.setCustomerId("inventory-load-customer-" + orderId);
                        
                        OrderItem item = new OrderItem();
                        item.setProductId(productId);
                        item.setQuantity(quantityPerOrder);
                        item.setPrice(BigDecimal.valueOf(99.99));
                        
                        request.setItems(Arrays.asList(item));

                        mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
                        
                        return true;
                    } catch (Exception e) {
                        // Some orders might fail due to insufficient inventory
                        return false;
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        // Count successful orders
        long successfulOrders = Arrays.stream(futures)
                .mapToLong(future -> {
                    try {
                        return future.get() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        System.out.println("Successfully processed " + successfulOrders + " out of " + numberOfConcurrentOrders + " concurrent orders");

        // Verify that inventory was properly managed (no overselling)
        // The exact number depends on initial inventory, but there should be some successful orders
        assertTrue(successfulOrders > 0, "At least some orders should succeed");
        assertTrue(successfulOrders <= numberOfConcurrentOrders, "Cannot have more successful orders than attempted");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testCachePerformance() throws Exception {
        // Create an order to cache
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleOrderRequest)))
                .andExpect(status().isCreated());

        // Get the order ID from customer query (this will cache the result)
        String customerId = sampleOrderRequest.getCustomerId();
        
        // First query (cache miss)
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/query/orders/customer/" + customerId))
                .andExpect(status().isOk());
        long firstQueryTime = System.currentTimeMillis() - startTime;

        // Subsequent queries (cache hits)
        long cacheStartTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/query/orders/customer/" + customerId))
                    .andExpect(status().isOk());
        }
        long cacheQueriesTime = System.currentTimeMillis() - cacheStartTime;
        double averageCacheQueryTime = (double) cacheQueriesTime / 10;

        System.out.println("First query (cache miss): " + firstQueryTime + "ms");
        System.out.println("Average cached query time: " + averageCacheQueryTime + "ms");

        // Cache should significantly improve performance
        assertTrue(averageCacheQueryTime < firstQueryTime, 
                  "Cached queries should be faster than the first query");
    }

    @Test
    void testMemoryUsageUnderLoad() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // Get initial memory usage
        runtime.gc(); // Suggest garbage collection
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many orders to test memory usage
        int numberOfOrders = 200;
        for (int i = 0; i < numberOfOrders; i++) {
            OrderRequest request = new OrderRequest();
            request.setCustomerId("memory-test-customer-" + i);
            
            OrderItem item = new OrderItem();
            item.setProductId("product-1");
            item.setQuantity(1);
            item.setPrice(BigDecimal.valueOf(99.99));
            
            request.setItems(Arrays.asList(item));

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // Get memory usage after load
        runtime.gc(); // Suggest garbage collection
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory usage: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        // Memory increase should be reasonable (adjust threshold as needed)
        long maxAcceptableIncrease = 100 * 1024 * 1024; // 100 MB
        assertTrue(memoryIncrease < maxAcceptableIncrease, 
                  "Memory increase should be less than 100MB for " + numberOfOrders + " orders");
    }
}