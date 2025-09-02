package com.ordersystem.unified.order.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderItemEntity.
 */
class OrderItemEntityTest {

    private OrderItemEntity orderItem;

    @BeforeEach
    void setUp() {
        orderItem = new OrderItemEntity("product-123", "Test Product", 3, new BigDecimal("25.50"));
    }

    @Test
    void shouldCreateOrderItemWithCorrectValues() {
        assertThat(orderItem.getProductId()).isEqualTo("product-123");
        assertThat(orderItem.getProductName()).isEqualTo("Test Product");
        assertThat(orderItem.getQuantity()).isEqualTo(3);
        assertThat(orderItem.getPrice()).isEqualTo(new BigDecimal("25.50"));
    }

    @Test
    void shouldCalculateTotalPriceCorrectly() {
        BigDecimal totalPrice = orderItem.getTotalPrice();

        assertThat(totalPrice).isEqualTo(new BigDecimal("76.50")); // 3 * 25.50
    }

    @Test
    void shouldReturnZeroTotalPriceWhenQuantityIsNull() {
        orderItem.setQuantity(null);

        BigDecimal totalPrice = orderItem.getTotalPrice();

        assertThat(totalPrice).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroTotalPriceWhenPriceIsNull() {
        orderItem.setPrice(null);

        BigDecimal totalPrice = orderItem.getTotalPrice();

        assertThat(totalPrice).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldHandleZeroQuantity() {
        orderItem.setQuantity(0);

        BigDecimal totalPrice = orderItem.getTotalPrice();

        assertThat(totalPrice).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldHandleZeroPrice() {
        orderItem.setPrice(BigDecimal.ZERO);

        BigDecimal totalPrice = orderItem.getTotalPrice();

        assertThat(totalPrice).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        OrderItemEntity item1 = new OrderItemEntity("product-1", "Product 1", 1, new BigDecimal("10.00"));
        OrderItemEntity item2 = new OrderItemEntity("product-2", "Product 2", 2, new BigDecimal("20.00"));
        
        // Set same ID to test equality
        item1.setId(1L);
        item2.setId(1L);
        OrderItemEntity item3 = new OrderItemEntity("product-3", "Product 3", 3, new BigDecimal("30.00"));
        item3.setId(2L);

        assertThat(item1).isEqualTo(item2); // Same ID
        assertThat(item1).isNotEqualTo(item3); // Different ID
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
    }

    @Test
    void shouldGenerateCorrectToString() {
        orderItem.setId(123L);
        String toString = orderItem.toString();

        assertThat(toString).contains("123");
        assertThat(toString).contains("product-123");
        assertThat(toString).contains("Test Product");
        assertThat(toString).contains("3");
        assertThat(toString).contains("25.50");
    }

    @Test
    void shouldMaintainOrderRelationship() {
        Order order = new Order("order-1", "customer-1", "Customer 1", new BigDecimal("100.00"));
        
        orderItem.setOrder(order);

        assertThat(orderItem.getOrder()).isEqualTo(order);
    }
}