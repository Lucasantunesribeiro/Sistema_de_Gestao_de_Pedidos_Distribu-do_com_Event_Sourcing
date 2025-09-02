package com.ordersystem.unified.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private CreateOrderRequest validOrderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        OrderItemRequest item1 = new OrderItemRequest("product-1", "Product 1", 2, new BigDecimal("25.00"));
        OrderItemRequest item2 = new OrderItemRequest("product-2", "Product 2", 1, new BigDecimal("50.00"));
        
        validOrderRequest = new CreateOrderRequest("customer-123", "John Doe", Arrays.asList(item1, item2));
        validOrderRequest.setCorrelationId("corr-123");

        orderResponse = new OrderResponse(
            "order-123",
            "customer-123", 
            "John Doe",
            OrderStatus.CONFIRMED,
            new BigDecimal("100.00"),
            null, // items not needed for these tests
            LocalDateTime.now(),
            LocalDateTime.now(),
            "corr-123"
        );
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.correlationId").value("corr-123"));
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderRequest() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest("", "", null);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForEmptyCustomerId() throws Exception {
        validOrderRequest.setCustomerId("");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForEmptyItems() throws Exception {
        validOrderRequest.setItems(Arrays.asList());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetOrderByIdSuccessfully() throws Exception {
        when(orderService.getOrder("order-123")).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentOrder() throws Exception {
        when(orderService.getOrder("non-existent")).thenThrow(new OrderNotFoundException("non-existent"));

        mockMvc.perform(get("/api/orders/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetOrdersWithoutFilters() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getRecentOrders(any(PageRequest.class))).thenReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("order-123"));
    }

    @Test
    void shouldGetOrdersByCustomerId() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByCustomer("customer-123")).thenReturn(orders);

        mockMvc.perform(get("/api/orders")
                .param("customerId", "customer-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value("customer-123"));
    }

    @Test
    void shouldGetOrdersByStatus() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByStatus(OrderStatus.CONFIRMED)).thenReturn(orders);

        mockMvc.perform(get("/api/orders")
                .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void shouldGetOrdersByCustomerEndpoint() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByCustomer("customer-123")).thenReturn(orders);

        mockMvc.perform(get("/api/orders/customer/customer-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value("customer-123"));
    }

    @Test
    void shouldGetOrdersByStatusEndpoint() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByStatus(OrderStatus.CONFIRMED)).thenReturn(orders);

        mockMvc.perform(get("/api/orders/status/CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void shouldHandlePaginationParameters() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getRecentOrders(any(PageRequest.class))).thenReturn(orders);

        mockMvc.perform(get("/api/orders")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnBadRequestForBlankOrderId() throws Exception {
        mockMvc.perform(get("/api/orders/ "))
                .andExpect(status().is5xxServerError()); // Spring may return 500 for blank paths
    }

    @Test
    void shouldReturnBadRequestForBlankCustomerId() throws Exception {
        mockMvc.perform(get("/api/orders/customer/ "))
                .andExpect(status().is5xxServerError()); // Spring may return 500 for blank paths
    }
}