package com.ordersystem.unified.order.repository;

import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderRepository.
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    void setUp() {
        // Create test orders
        order1 = new Order("order-1", "customer-1", "John Doe", new BigDecimal("100.00"));
        order1.setStatus(OrderStatus.PENDING);
        order1.setCorrelationId("corr-1");

        order2 = new Order("order-2", "customer-1", "John Doe", new BigDecimal("200.00"));
        order2.setStatus(OrderStatus.CONFIRMED);
        order2.setCorrelationId("corr-2");

        order3 = new Order("order-3", "customer-2", "Jane Smith", new BigDecimal("150.00"));
        order3.setStatus(OrderStatus.PENDING);
        order3.setCorrelationId("corr-3");

        // Add items to orders
        OrderItemEntity item1 = new OrderItemEntity("product-1", "Product 1", 2, new BigDecimal("50.00"));
        OrderItemEntity item2 = new OrderItemEntity("product-2", "Product 2", 1, new BigDecimal("200.00"));
        OrderItemEntity item3 = new OrderItemEntity("product-3", "Product 3", 3, new BigDecimal("50.00"));

        order1.addItem(item1);
        order2.addItem(item2);
        order3.addItem(item3);

        // Persist orders
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        List<Order> orders = orderRepository.findByCustomerId("customer-1");

        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getId).containsExactlyInAnyOrder("order-1", "order-2");
    }

    @Test
    void shouldFindOrdersByStatus() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> confirmedOrders = orderRepository.findByStatus(OrderStatus.CONFIRMED);

        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).extracting(Order::getId).containsExactlyInAnyOrder("order-1", "order-3");
        
        assertThat(confirmedOrders).hasSize(1);
        assertThat(confirmedOrders).extracting(Order::getId).containsExactly("order-2");
    }

    @Test
    void shouldFindOrdersByCustomerIdAndStatus() {
        List<Order> orders = orderRepository.findByCustomerIdAndStatus("customer-1", OrderStatus.PENDING);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getId()).isEqualTo("order-1");
    }

    @Test
    void shouldFindOrdersByCorrelationId() {
        Optional<Order> order = orderRepository.findByCorrelationId("corr-2");

        assertThat(order).isPresent();
        assertThat(order.get().getId()).isEqualTo("order-2");
    }

    @Test
    void shouldCountOrdersByStatus() {
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedCount = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledCount = orderRepository.countByStatus(OrderStatus.CANCELLED);

        assertThat(pendingCount).isEqualTo(2);
        assertThat(confirmedCount).isEqualTo(1);
        assertThat(cancelledCount).isEqualTo(0);
    }

    @Test
    void shouldFindRecentOrders() {
        List<Order> recentOrders = orderRepository.findRecentOrders(PageRequest.of(0, 2));

        assertThat(recentOrders).hasSize(2);
        // Orders should be sorted by creation date descending
        assertThat(recentOrders.get(0).getCreatedAt()).isAfterOrEqualTo(recentOrders.get(1).getCreatedAt());
    }

    @Test
    void shouldFindNonTerminalOrders() {
        List<Order> nonTerminalOrders = orderRepository.findNonTerminalOrders();

        assertThat(nonTerminalOrders).hasSize(2);
        assertThat(nonTerminalOrders).extracting(Order::getId).containsExactlyInAnyOrder("order-1", "order-3");
        assertThat(nonTerminalOrders).allMatch(order -> !order.isTerminal());
    }

    @Test
    void shouldFindOrdersCreatedBetween() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        List<Order> orders = orderRepository.findOrdersCreatedBetween(start, end);

        assertThat(orders).hasSize(3);
        assertThat(orders).allMatch(order -> 
            order.getCreatedAt().isAfter(start) && order.getCreatedAt().isBefore(end));
    }

    @Test
    void shouldCheckOrderExistence() {
        boolean exists = orderRepository.existsById("order-1");
        boolean notExists = orderRepository.existsById("non-existent");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldSaveAndRetrieveOrderWithItems() {
        Order newOrder = new Order("order-4", "customer-3", "Bob Wilson", new BigDecimal("300.00"));
        OrderItemEntity item = new OrderItemEntity("product-4", "Product 4", 2, new BigDecimal("150.00"));
        newOrder.addItem(item);

        Order savedOrder = orderRepository.save(newOrder);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> retrievedOrder = orderRepository.findById("order-4");

        assertThat(retrievedOrder).isPresent();
        assertThat(retrievedOrder.get().getItems()).hasSize(1);
        assertThat(retrievedOrder.get().getItems().get(0).getProductId()).isEqualTo("product-4");
    }

    @Test
    void shouldDeleteOrderCascadingItems() {
        orderRepository.deleteById("order-1");
        entityManager.flush();

        Optional<Order> deletedOrder = orderRepository.findById("order-1");
        assertThat(deletedOrder).isEmpty();

        // Verify items are also deleted (cascade)
        List<OrderItemEntity> remainingItems = entityManager.getEntityManager()
            .createQuery("SELECT i FROM OrderItemEntity i WHERE i.order.id = 'order-1'", OrderItemEntity.class)
            .getResultList();
        assertThat(remainingItems).isEmpty();
    }
}