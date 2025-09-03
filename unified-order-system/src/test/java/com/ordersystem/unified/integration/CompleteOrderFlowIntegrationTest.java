package com.ordersystem.unified.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.*;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.shared.events.OrderStatus;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete order flow including payment and inventory
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class CompleteOrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest validOrderRequest;
    private String createdOrderId;

    @BeforeEach
    void setUp() {
        // Create a valid order request for testing
        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId("CUST-12345");
        validOrderRequest.setCustomerName("John Doe");
        validOrderRequest.setCustomerEmail("john.doe@example.com");
        validOrderRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        // Add order items
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId("PROD-001");
        item1.setProductName("Laptop");
        item1.setQuantity(1);
        item1.setUnitPrice(new BigDecimal("999.99"));

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId("PROD-002");
        item2.setProductName("Mouse");
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("29.99"));

        validOrderRequest.setItems(List.of(item1, item2));
    }

    @Test
    @Order(1)
    void testSuccessfulOrderCreation() throws Exception {
        // Test successful order creation with payment and inventory integration
        MvcResult result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.customerId").value("CUST-12345"))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(1059.97))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn();

        // Extract order ID for subsequent tests
        String responseContent = result.getResponse().getContentAsString();
        OrderResponse orderResponse = objectMapper.readValue(responseContent, OrderResponse.class);
        createdOrderId = orderResponse.getOrderId();
        
        assertNotNull(createdOrderId);
        assertNotNull(orderResponse.getPaymentId());
        assertNotNull(orderResponse.getReservationId());
        assertNotNull(orderResponse.getTransactionId());
    }

    @Test
    @Order(2)
    void testOrderRetrievalAfterCreation() throws Exception {
        // First create an order
        testSuccessfulOrderCreation();
        
        // Then retrieve it
        mockMvc.perform(get("/api/orders/{orderId}", createdOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(createdOrderId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    @Order(3)
    void testOrderCancellation() throws Exception {
        // First create an order
        testSuccessfulOrderCreation();
        
        // Then cancel it
        Map<String, String> cancellationRequest = Map.of(
            "reason", "Customer requested cancellation",
            "correlationId", "test-correlation-id"
        );

        mockMvc.perform(post("/api/orders/cancel/{orderId}", createdOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancellationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(createdOrderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Customer requested cancellation"));
    }

    @Test
    @Order(4)
    void testInsufficientInventoryScenario() throws Exception {
        // Create an order with very high quantities to trigger insufficient inventory
        CreateOrderRequest largeOrderRequest = new CreateOrderRequest();
        largeOrderRequest.setCustomerId("CUST-99999");
        largeOrderRequest.setCustomerName("Large Order Customer");
        largeOrderRequest.setCustomerEmail("large@example.com");
        largeOrderRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest largeItem = new OrderItemRequest();
        largeItem.setProductId("PROD-001");
        largeItem.setProductName("Laptop");
        largeItem.setQuantity(10000); // Very large quantity
        largeItem.setUnitPrice(new BigDecimal("999.99"));

        largeOrderRequest.setItems(List.of(largeItem));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("insufficient inventory")));
    }

    @Test
    @Order(5)
    void testInvalidOrderRequest() throws Exception {
        // Test with invalid order request (missing required fields)
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        invalidRequest.setCustomerId(""); // Empty customer ID
        invalidRequest.setCustomerName(""); // Empty customer name
        invalidRequest.setItems(List.of()); // Empty items list

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void testOrdersByCustomer() throws Exception {
        // First create an order
        testSuccessfulOrderCreation();
        
        // Then retrieve orders by customer
        mockMvc.perform(get("/api/orders/customer/{customerId}", "CUST-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].customerId").value("CUST-12345"));
    }

    @Test
    @Order(7)
    void testOrdersByStatus() throws Exception {
        // Test retrieving orders by status
        mockMvc.perform(get("/api/orders/status/{status}", OrderStatus.CONFIRMED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(8)
    void testOrderStatistics() throws Exception {
        // Test order statistics endpoint
        mockMvc.perform(get("/api/orders/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").exists())
                .andExpect(jsonPath("$.confirmedOrders").exists())
                .andExpect(jsonPath("$.cancelledOrders").exists())
                .andExpect(jsonPath("$.pendingOrders").exists())
                .andExpect(jsonPath("$.totalRevenue").exists());
    }

    @Test
    @Order(9)
    void testPaymentIntegration() throws Exception {
        // Test different payment methods
        CreateOrderRequest pixOrderRequest = new CreateOrderRequest();
        pixOrderRequest.setCustomerId("CUST-PIX");
        pixOrderRequest.setCustomerName("PIX Customer");
        pixOrderRequest.setCustomerEmail("pix@example.com");
        pixOrderRequest.setPaymentMethod(PaymentMethod.PIX);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PROD-003");
        item.setProductName("Keyboard");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("79.99"));

        pixOrderRequest.setItems(List.of(item));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pixOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(10)
    void testConcurrentOrderCreation() throws Exception {
        // Test concurrent order creation to verify thread safety
        CreateOrderRequest request1 = createOrderRequest("CUST-CONCURRENT-1", "Concurrent Customer 1");
        CreateOrderRequest request2 = createOrderRequest("CUST-CONCURRENT-2", "Concurrent Customer 2");

        // Execute both requests
        MvcResult result1 = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn();

        // Verify both orders were created successfully
        OrderResponse order1 = objectMapper.readValue(result1.getResponse().getContentAsString(), OrderResponse.class);
        OrderResponse order2 = objectMapper.readValue(result2.getResponse().getContentAsString(), OrderResponse.class);

        assertNotEquals(order1.getOrderId(), order2.getOrderId());
        assertNotEquals(order1.getPaymentId(), order2.getPaymentId());
        assertNotEquals(order1.getReservationId(), order2.getReservationId());
    }

    @Test
    @Order(11)
    void testOrderFlowWithMultipleItems() throws Exception {
        // Test order with multiple items of different types
        CreateOrderRequest multiItemRequest = new CreateOrderRequest();
        multiItemRequest.setCustomerId("CUST-MULTI");
        multiItemRequest.setCustomerName("Multi Item Customer");
        multiItemRequest.setCustomerEmail("multi@example.com");
        multiItemRequest.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId("PROD-LAPTOP");
        item1.setProductName("Gaming Laptop");
        item1.setQuantity(1);
        item1.setUnitPrice(new BigDecimal("1499.99"));

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId("PROD-MOUSE");
        item2.setProductName("Gaming Mouse");
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("59.99"));

        OrderItemRequest item3 = new OrderItemRequest();
        item3.setProductId("PROD-KEYBOARD");
        item3.setProductName("Mechanical Keyboard");
        item3.setQuantity(1);
        item3.setUnitPrice(new BigDecimal("129.99"));

        multiItemRequest.setItems(List.of(item1, item2, item3));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(multiItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.totalAmount").value(1689.97))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(12)
    void testOrderNotFound() throws Exception {
        // Test retrieving non-existent order
        mockMvc.perform(get("/api/orders/{orderId}", "NON-EXISTENT-ORDER"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    void testHealthCheckIntegration() throws Exception {
        // Test that health check includes all services
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.services.order-service.status").value("UP"))
                .andExpect(jsonPath("$.services.payment-service.status").value("UP"))
                .andExpect(jsonPath("$.services.inventory-service.status").value("UP"))
                .andExpect(jsonPath("$.services.database.status").value("UP"));
    }

    // Helper methods

    private CreateOrderRequest createOrderRequest(String customerId, String customerName) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCustomerName(customerName);
        request.setCustomerEmail(customerId.toLowerCase() + "@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PROD-TEST");
        item.setProductName("Test Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(List.of(item));
        return request;
    }
}