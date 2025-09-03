package com.ordersystem.unified.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.*;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load testing for the complete order system
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class PerformanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest baseOrderRequest;

    @BeforeEach
    void setUp() {
        // Create a base order request for performance testing
        baseOrderRequest = new CreateOrderRequest();
        baseOrderRequest.setCustomerName("Performance Test Customer");
        baseOrderRequest.setCustomerEmail("perf@example.com");
        baseOrderRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PERF-PROD-001");
        item.setProductName("Performance Test Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        baseOrderRequest.setItems(List.of(item));
    }

    @Test
    @Order(1)
    void testConcurrentOrderCreation() throws Exception {
        int numberOfOrders = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = IntStream.range(0, numberOfOrders)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    CreateOrderRequest request = createUniqueOrderRequest(i);
                    
                    MvcResult result = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated())
                            .andReturn();
                    
                    // Verify response
                    String responseContent = result.getResponse().getContentAsString();
                    OrderResponse orderResponse = objectMapper.readValue(responseContent, OrderResponse.class);
                    assertNotNull(orderResponse.getOrderId());
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create order " + i, e);
                }
            }, executor))
            .toArray(CompletableFuture[]::new);
        
        // Wait for all orders to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        
        System.out.println("Created " + numberOfOrders + " orders in " + totalTime + "ms");
        System.out.println("Average time per order: " + (totalTime / numberOfOrders) + "ms");
        
        // Assert reasonable performance (less than 5 seconds total for 10 orders)
        assertTrue(totalTime < 5000, "Order creation took too long: " + totalTime + "ms");
    }

    @Test
    @Order(2)
    void testHighVolumeOrderRetrieval() throws Exception {
        // First create multiple orders
        int numberOfOrders = 20;
        for (int i = 0; i < numberOfOrders; i++) {
            CreateOrderRequest request = createUniqueOrderRequest(i);
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // Then test retrieval performance
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/orders")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("Performed 50 order retrievals in " + totalTime + "ms");
        System.out.println("Average time per retrieval: " + (totalTime / 50) + "ms");
        
        // Assert reasonable performance (less than 2 seconds total for 50 retrievals)
        assertTrue(totalTime < 2000, "Order retrieval took too long: " + totalTime + "ms");
    }

    @Test
    @Order(3)
    void testMemoryUsageUnderLoad() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // Get initial memory usage
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many orders to test memory usage
        int numberOfOrders = 100;
        for (int i = 0; i < numberOfOrders; i++) {
            CreateOrderRequest request = createUniqueOrderRequest(i);
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            
            // Force garbage collection every 20 orders
            if (i % 20 == 0) {
                System.gc();
                Thread.sleep(100);
            }
        }
        
        // Force garbage collection and wait
        System.gc();
        Thread.sleep(1000);
        
        // Get final memory usage
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory usage: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        // Assert memory usage is reasonable (less than 100MB increase)
        assertTrue(memoryIncrease < 100 * 1024 * 1024, 
                  "Memory usage increased too much: " + (memoryIncrease / 1024 / 1024) + " MB");
    }

    @Test
    @Order(4)
    void testHealthCheckPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Perform multiple health checks
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("Performed 100 health checks in " + totalTime + "ms");
        System.out.println("Average time per health check: " + (totalTime / 100) + "ms");
        
        // Assert health checks are fast (less than 1 second total for 100 checks)
        assertTrue(totalTime < 1000, "Health checks took too long: " + totalTime + "ms");
    }

    @Test
    @Order(5)
    void testDatabaseConnectionPooling() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        long startTime = System.currentTimeMillis();
        
        // Create many concurrent database operations
        CompletableFuture<?>[] futures = IntStream.range(0, 50)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    // Test order statistics (database-heavy operation)
                    mockMvc.perform(get("/api/orders/statistics"))
                            .andExpect(status().isOk());
                    
                    // Test health check (includes database check)
                    mockMvc.perform(get("/api/health"))
                            .andExpect(status().isOk());
                    
                } catch (Exception e) {
                    throw new RuntimeException("Database operation failed", e);
                }
            }, executor))
            .toArray(CompletableFuture[]::new);
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        
        System.out.println("Performed 50 concurrent database operations in " + totalTime + "ms");
        
        // Assert reasonable performance (less than 10 seconds for 50 concurrent operations)
        assertTrue(totalTime < 10000, "Database operations took too long: " + totalTime + "ms");
    }

    @Test
    @Order(6)
    void testServiceIntegrationPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Test complete order flow performance (includes payment and inventory)
        for (int i = 0; i < 10; i++) {
            CreateOrderRequest request = createUniqueOrderRequest(i);
            
            MvcResult result = mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            // Verify the order was processed completely
            String responseContent = result.getResponse().getContentAsString();
            OrderResponse orderResponse = objectMapper.readValue(responseContent, OrderResponse.class);
            
            assertNotNull(orderResponse.getPaymentId());
            assertNotNull(orderResponse.getReservationId());
            assertNotNull(orderResponse.getTransactionId());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("Created 10 complete orders (with payment and inventory) in " + totalTime + "ms");
        System.out.println("Average time per complete order: " + (totalTime / 10) + "ms");
        
        // Assert reasonable performance for complete order flow (less than 10 seconds for 10 orders)
        assertTrue(totalTime < 10000, "Complete order flow took too long: " + totalTime + "ms");
    }

    @Test
    @Order(7)
    void testErrorHandlingPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Test performance of error handling with invalid requests
        for (int i = 0; i < 50; i++) {
            CreateOrderRequest invalidRequest = new CreateOrderRequest();
            invalidRequest.setCustomerId(""); // Invalid
            invalidRequest.setCustomerName(""); // Invalid
            invalidRequest.setItems(List.of()); // Invalid
            
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("Handled 50 invalid requests in " + totalTime + "ms");
        System.out.println("Average time per error handling: " + (totalTime / 50) + "ms");
        
        // Assert error handling is fast (less than 2 seconds for 50 invalid requests)
        assertTrue(totalTime < 2000, "Error handling took too long: " + totalTime + "ms");
    }

    // Helper methods

    private CreateOrderRequest createUniqueOrderRequest(int index) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("PERF-CUST-" + index);
        request.setCustomerName("Performance Customer " + index);
        request.setCustomerEmail("perf" + index + "@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PERF-PROD-" + index);
        item.setProductName("Performance Product " + index);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(List.of(item));
        return request;
    }
}