package com.ordersystem.unified.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.OrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.shared.events.OrderItem;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete order workflow.
 * Tests the entire flow from order creation to completion.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class OrderWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRequest sampleOrderRequest;

    @BeforeEach
    void setUp() {
        // Create sample order request
        sampleOrderRequest = new OrderRequest();
        sampleOrderRequest.setCustomerId("customer-123");
        
        OrderItem item1 = new OrderItem();
        item1.setProductId("product-1");
        item1.setQuantity(2);
        item1.setPrice(BigDecimal.valueOf(99.99));
        
        OrderItem item2 = new OrderItem();
        item2.setProductId("product-2");
        item2.setQuantity(1);
        item2.setPrice(BigDecimal.valueOf(49.99));
        
        sampleOrderRequest.setItems(Arrays.asList(item1, item2));
    }

    @Test
    void testCompleteOrderWorkflow() throws Exception {
        // Step 1: Create order
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        OrderResponse createdOrder = objectMapper.readValue(responseContent, OrderResponse.class);
        String orderId = createdOrder.getOrderId();

        assertNotNull(orderId);
        assertEquals(OrderStatus.PENDING, createdOrder.getStatus());
        assertEquals(2, createdOrder.getItems().size());

        // Step 2: Verify order was created and can be retrieved
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpected(jsonPath("$.status").value("PENDING"));

        // Step 3: Process order (this should trigger inventory reservation and payment)
        mockMvc.perform(post("/api/orders/{orderId}/process", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Step 4: Verify order status changed to CONFIRMED
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Step 5: Verify inventory was reserved
        mockMvc.perform(get("/api/query/inventory/product-1/quantity"))
                .andExpect(status().isOk())
                .andExpect(content().string("98")); // 100 - 2 = 98

        // Step 6: Verify payment was processed
        mockMvc.perform(get("/api/query/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.successful").value(true));

        // Step 7: Query order summary
        mockMvc.perform(get("/api/query/orders/{orderId}/summary", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.orderId").value(orderId))
                .andExpect(jsonPath("$.hasSuccessfulPayment").value(true));
    }

    @Test
    void testOrderWorkflowWithInsufficientInventory() throws Exception {
        // Create order with high quantity that should fail
        OrderRequest largeOrderRequest = new OrderRequest();
        largeOrderRequest.setCustomerId("customer-456");
        
        OrderItem largeItem = new OrderItem();
        largeItem.setProductId("product-1");
        largeItem.setQuantity(1000); // More than available
        largeItem.setPrice(BigDecimal.valueOf(99.99));
        
        largeOrderRequest.setItems(Arrays.asList(largeItem));

        // Should fail due to insufficient inventory
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("Insufficient inventory")));
    }

    @Test
    void testOrderCancellation() throws Exception {
        // Create order
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        OrderResponse createdOrder = objectMapper.readValue(responseContent, OrderResponse.class);
        String orderId = createdOrder.getOrderId();

        // Cancel order
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                .param("reason", "Customer request"))
                .andExpect(status().isOk());

        // Verify order was cancelled
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testQueryEndpoints() throws Exception {
        // Create multiple orders for testing queries
        for (int i = 0; i < 3; i++) {
            OrderRequest request = new OrderRequest();
            request.setCustomerId("customer-query-test");
            
            OrderItem item = new OrderItem();
            item.setProductId("product-" + (i + 1));
            item.setQuantity(1);
            item.setPrice(BigDecimal.valueOf(10.00 + i));
            
            request.setItems(Arrays.asList(item));

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Test query by customer
        mockMvc.perform(get("/api/query/orders/customer/customer-query-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        // Test query by status
        mockMvc.perform(get("/api/query/orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Test inventory queries
        mockMvc.perform(get("/api/query/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/query/inventory/out-of-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testHealthEndpoints() throws Exception {
        // Test main health endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.redis.status").value("UP"));

        // Test custom health indicator
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.healthConfig.details.service").value("unified-order-system"));
    }

    @Test
    void testCorrelationIdPropagation() throws Exception {
        String correlationId = "test-correlation-123";

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("X-Correlation-ID", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-ID", correlationId))
                .andReturn();

        // Verify correlation ID is returned in response
        assertEquals(correlationId, result.getResponse().getHeader("X-Correlation-ID"));
    }

    @Test
    void testConcurrentOrderProcessing() throws Exception {
        // This test would ideally use multiple threads, but for simplicity
        // we'll test sequential processing of multiple orders
        
        for (int i = 0; i < 5; i++) {
            OrderRequest request = new OrderRequest();
            request.setCustomerId("customer-concurrent-" + i);
            
            OrderItem item = new OrderItem();
            item.setProductId("product-1");
            item.setQuantity(1);
            item.setPrice(BigDecimal.valueOf(99.99));
            
            request.setItems(Arrays.asList(item));

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        // Verify all orders were created
        mockMvc.perform(get("/api/query/orders")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));
    }
}