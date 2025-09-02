package com.ordersystem.unified.order;

import com.ordersystem.unified.inventory.InventoryResult;
import com.ordersystem.unified.inventory.InventoryService;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.payment.PaymentResult;
import com.ordersystem.unified.payment.PaymentService;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validOrderRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        // Setup valid order request
        List<OrderItemRequest> items = Arrays.asList(
            new OrderItemRequest("product-1", "Product 1", 2, new BigDecimal("25.00")),
            new OrderItemRequest("product-2", "Product 2", 1, new BigDecimal("50.00"))
        );
        
        validOrderRequest = new CreateOrderRequest("customer-123", "John Doe", items);
        validOrderRequest.setCorrelationId("correlation-123");

        // Setup saved order
        savedOrder = new Order("order-123", "customer-123", "John Doe", new BigDecimal("100.00"));
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());
        savedOrder.setCorrelationId("correlation-123");
        
        OrderItemEntity item1 = new OrderItemEntity("product-1", "Product 1", 2, new BigDecimal("25.00"));
        OrderItemEntity item2 = new OrderItemEntity("product-2", "Product 2", 1, new BigDecimal("50.00"));
        item1.setId(1L);
        item2.setId(2L);
        savedOrder.addItem(item1);
        savedOrder.addItem(item2);
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        when(inventoryService.reserveItems(any())).thenReturn(InventoryResult.success("product-1", 2));
        when(paymentService.processPayment(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(PaymentResult.success("payment-123"));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        OrderResponse response = orderService.createOrder(validOrderRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("customer-123");
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getItems()).hasSize(2);

        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        verify(inventoryService).reserveItems(any());
        verify(paymentService).processPayment(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void shouldFailOrderWhenInventoryReservationFails() {
        // Given
        when(inventoryService.reserveItems(any()))
            .thenReturn(InventoryResult.insufficientStock("product-1", 2, 1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(validOrderRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Inventory reservation failed");

        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        verify(inventoryService).reserveItems(any());
        verify(paymentService, never()).processPayment(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void shouldFailOrderWhenPaymentFails() {
        // Given
        when(inventoryService.reserveItems(any())).thenReturn(InventoryResult.success("product-1", 2));
        when(inventoryService.releaseItems(any())).thenReturn(InventoryResult.released("product-1", 2));
        when(paymentService.processPayment(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(PaymentResult.failure("Insufficient funds", "INSUFFICIENT_FUNDS"));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(validOrderRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Payment processing failed");

        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        verify(inventoryService).reserveItems(any());
        verify(inventoryService).releaseItems(any()); // Should release inventory on payment failure
        verify(paymentService).processPayment(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void shouldValidateOrderRequestWithEmptyItems() {
        // Given
        CreateOrderRequest invalidRequest = new CreateOrderRequest("customer-123", "John Doe", Arrays.asList());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Order must contain at least one item");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldValidateOrderRequestWithInvalidQuantity() {
        // Given
        List<OrderItemRequest> invalidItems = Arrays.asList(
            new OrderItemRequest("product-1", "Product 1", 0, new BigDecimal("25.00"))
        );
        CreateOrderRequest invalidRequest = new CreateOrderRequest("customer-123", "John Doe", invalidItems);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Item quantity must be positive");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldGetOrderById() {
        // Given
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(savedOrder));

        // When
        OrderResponse response = orderService.getOrder("order-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order-123");
        assertThat(response.getCustomerId()).isEqualTo("customer-123");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(orderRepository).findById("order-123");
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrder("non-existent"))
            .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository).findById("non-existent");
    }

    @Test
    void shouldGetOrdersByCustomer() {
        // Given
        List<Order> orders = Arrays.asList(savedOrder);
        when(orderRepository.findByCustomerId("customer-123")).thenReturn(orders);

        // When
        List<OrderResponse> responses = orderService.getOrdersByCustomer("customer-123");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerId()).isEqualTo("customer-123");

        verify(orderRepository).findByCustomerId("customer-123");
    }

    @Test
    void shouldGetOrdersByStatus() {
        // Given
        List<Order> orders = Arrays.asList(savedOrder);
        when(orderRepository.findByStatus(OrderStatus.CONFIRMED)).thenReturn(orders);

        // When
        List<OrderResponse> responses = orderService.getOrdersByStatus(OrderStatus.CONFIRMED);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(orderRepository).findByStatus(OrderStatus.CONFIRMED);
    }
}