package com.ordersystem.unified.order;

import com.ordersystem.unified.inventory.InventoryService;
import com.ordersystem.unified.inventory.dto.ReservationResponse;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.PaymentService;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.payment.dto.PaymentResponse;
import com.ordersystem.unified.payment.dto.PaymentStatus;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

/**
 * Unit tests for OrderService.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validOrderRequest;

    @BeforeEach
    void setUp() {
        // Setup valid order request
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
        validOrderRequest.setCorrelationId("correlation-123");
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        ReservationResponse reservationResponse = ReservationResponse.success("reservation-123", "order-123", null, null);
        
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId("payment-123");
        paymentResponse.setStatus(PaymentStatus.COMPLETED);
        
        when(inventoryService.reserveItems(any())).thenReturn(reservationResponse);
        when(paymentService.processPayment(any())).thenReturn(paymentResponse);

        // When
        OrderResponse response = orderService.createOrder(validOrderRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("customer-123");
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));

        verify(inventoryService).reserveItems(any());
        verify(paymentService).processPayment(any());
    }

    @Test
    void shouldFailOrderWhenInventoryReservationFails() {
        // Given
        ReservationResponse reservationResponse = ReservationResponse.failure("reservation-123", "order-123", "Insufficient stock");
        
        when(inventoryService.reserveItems(any())).thenReturn(reservationResponse);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(validOrderRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Inventory reservation failed");

        verify(inventoryService).reserveItems(any());
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    void shouldFailOrderWhenPaymentFails() {
        // Given
        ReservationResponse reservationResponse = ReservationResponse.success("reservation-123", "order-123", null, null);
        
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId("payment-123");
        paymentResponse.setStatus(PaymentStatus.FAILED);
        paymentResponse.setErrorMessage("Insufficient funds");
        
        when(inventoryService.reserveItems(any())).thenReturn(reservationResponse);
        when(paymentService.processPayment(any())).thenReturn(paymentResponse);
        doNothing().when(inventoryService).releaseItems(anyList(), anyString());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(validOrderRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Payment processing failed");

        verify(inventoryService).reserveItems(any());
        verify(paymentService).processPayment(any());
        verify(inventoryService).releaseItems(anyList(), anyString()); // Should release inventory on payment failure
    }

    @Test
    void shouldValidateOrderRequestWithEmptyItems() {
        // Given
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        invalidRequest.setCustomerId("customer-123");
        invalidRequest.setCustomerName("John Doe");
        invalidRequest.setCustomerEmail("john@example.com");
        invalidRequest.setItems(Arrays.asList());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Order must contain at least one item");
    }

    @Test
    void shouldValidateOrderRequestWithInvalidQuantity() {
        // Given
        OrderItemRequest invalidItem = new OrderItemRequest();
        invalidItem.setProductId("product-1");
        invalidItem.setProductName("Product 1");
        invalidItem.setQuantity(0); // Invalid quantity
        invalidItem.setUnitPrice(new BigDecimal("25.00"));
        
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        invalidRequest.setCustomerId("customer-123");
        invalidRequest.setCustomerName("John Doe");
        invalidRequest.setCustomerEmail("john@example.com");
        invalidRequest.setItems(Arrays.asList(invalidItem));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Item quantity must be positive");
    }

    @Test
    void shouldGetOrderById() {
        // When
        OrderResponse response = orderService.getOrder("order-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order-123");
    }

    @Test
    void shouldGetOrdersByCustomer() {
        // When
        List<OrderResponse> responses = orderService.getOrdersByCustomer("customer-123");

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerId()).isEqualTo("customer-123");
    }

    @Test
    void shouldGetOrdersByStatus() {
        // When
        List<OrderResponse> responses = orderService.getOrdersByStatus("CONFIRMED");

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}