package com.ordersystem.unified.order.model;

import com.ordersystem.unified.shared.events.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Order entity.
 */
class OrderTest {

    private Order order;
    private OrderItemEntity item1;
    private OrderItemEntity item2;

    @BeforeEach
    void setUp() {
        order = new Order("order-123", "customer-456", "John Doe", new BigDecimal("100.00"));
        item1 = new OrderItemEntity("product-1", "Product 1", 2, new BigDecimal("25.00"));
        item2 = new OrderItemEntity("product-2", "Product 2", 1, new BigDecimal("50.00"));
    }

    @Test
    void shouldCreateOrderWithCorrectInitialState() {
        assertThat(order.getId()).isEqualTo("order-123");
        assertThat(order.getCustomerId()).isEqualTo("customer-456");
        assertThat(order.getCustomerName()).isEqualTo("John Doe");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void shouldAddItemsCorrectly() {
        order.addItem(item1);
        order.addItem(item2);

        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems()).contains(item1, item2);
        assertThat(item1.getOrder()).isEqualTo(order);
        assertThat(item2.getOrder()).isEqualTo(order);
    }

    @Test
    void shouldRemoveItemsCorrectly() {
        order.addItem(item1);
        order.addItem(item2);
        
        order.removeItem(item1);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems()).contains(item2);
        assertThat(order.getItems()).doesNotContain(item1);
        assertThat(item1.getOrder()).isNull();
    }

    @Test
    void shouldUpdateStatusCorrectly() {
        order.updateStatus(OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldIdentifyTerminalStatus() {
        order.updateStatus(OrderStatus.PENDING);
        assertThat(order.isTerminal()).isFalse();

        order.updateStatus(OrderStatus.CONFIRMED);
        assertThat(order.isTerminal()).isTrue();

        order.updateStatus(OrderStatus.CANCELLED);
        assertThat(order.isTerminal()).isTrue();

        order.updateStatus(OrderStatus.FAILED);
        assertThat(order.isTerminal()).isTrue();
    }

    @Test
    void shouldIdentifySuccessfulStatus() {
        order.updateStatus(OrderStatus.PENDING);
        assertThat(order.isSuccessful()).isFalse();

        order.updateStatus(OrderStatus.CONFIRMED);
        assertThat(order.isSuccessful()).isTrue();

        order.updateStatus(OrderStatus.CANCELLED);
        assertThat(order.isSuccessful()).isFalse();
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        Order order1 = new Order("order-123", "customer-1", "Customer 1", new BigDecimal("100.00"));
        Order order2 = new Order("order-123", "customer-2", "Customer 2", new BigDecimal("200.00"));
        Order order3 = new Order("order-456", "customer-1", "Customer 1", new BigDecimal("100.00"));

        assertThat(order1).isEqualTo(order2); // Same ID
        assertThat(order1).isNotEqualTo(order3); // Different ID
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }

    @Test
    void shouldGenerateCorrectToString() {
        order.addItem(item1);
        String toString = order.toString();

        assertThat(toString).contains("order-123");
        assertThat(toString).contains("customer-456");
        assertThat(toString).contains("PENDING");
        assertThat(toString).contains("100.00");
        assertThat(toString).contains("itemCount=1");
    }
}