package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validOrderRequest;

    @BeforeEach
    void setUp() {
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId("product-1");
        item1.setProductName("Product 1");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("25.00"));

        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId("customer-123");
        validOrderRequest.setCustomerName("John Doe");
        validOrderRequest.setItems(Arrays.asList(item1));
        validOrderRequest.setCorrelationId("correlation-123");
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return order;
        });

        // When
        OrderResponse response = orderService.createOrder(validOrderRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("customer-123");
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("50.00"));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldGetOrderById() {
        // Given
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, "cust-1", "Name", BigDecimal.TEN);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        OrderResponse response = orderService.getOrder(orderId);

        // Then
        assertThat(response.getOrderId()).isEqualTo(orderId);
    }
    
    @Test
    void shouldThrowWhenOrderNotFound() {
        // Given
        String orderId = "invalid-id";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrder(orderId))
            .isInstanceOf(OrderNotFoundException.class);
    }
}