package com.ordersystem.unified.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * Unit tests for OrderController.
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
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId("product-1");
        item1.setProductName("Product 1");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("25.00"));

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId("product-2");
        item2.setProductName("Product 2");
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("50.00"));
        
        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId("customer-123");
        validOrderRequest.setCustomerName("John Doe");
        validOrderRequest.setCustomerEmail("john@example.com");
        validOrderRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        validOrderRequest.setItems(Arrays.asList(item1, item2));
        validOrderRequest.setCorrelationId("corr-123");

        orderResponse = new OrderResponse();
        orderResponse.setOrderId("order-123");
        orderResponse.setCustomerId("customer-123");
        orderResponse.setCustomerName("John Doe");
        orderResponse.setStatus(OrderStatus.CONFIRMED);
        orderResponse.setTotalAmount(new BigDecimal("100.00"));
        orderResponse.setCreatedAt(LocalDateTime.now());
        orderResponse.setUpdatedAt(LocalDateTime.now());
        orderResponse.setCorrelationId("corr-123");
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
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        invalidRequest.setCustomerName("");
        invalidRequest.setCustomerEmail("");
        invalidRequest.setItems(null);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
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
    void shouldGetAllOrders() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrders(any(), any(), anyInt(), anyInt())).thenReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("order-123"));
    }

    @Test
    void shouldGetOrdersByCustomerId() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByCustomer("customer-123")).thenReturn(orders);

        mockMvc.perform(get("/api/orders/customer/customer-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value("customer-123"));
    }

    @Test
    void shouldGetOrdersByStatus() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByStatus("CONFIRMED")).thenReturn(orders);

        mockMvc.perform(get("/api/orders/status/CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }
}