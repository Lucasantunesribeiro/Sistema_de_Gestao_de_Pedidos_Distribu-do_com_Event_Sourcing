package com.ordersystem.unified.order;

import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests to verify PostgreSQL integration
 */
@DataJpaTest
@ActiveProfiles("test")
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void testSaveAndRetrieveOrder() {
        // Create order
        Order order = new Order("test-order-1", "CUST-123", "João Silva", BigDecimal.valueOf(2500.00));
        order.setCorrelationId("test-correlation-1");
        
        // Add item
        OrderItemEntity item = new OrderItemEntity("PROD-1", "Notebook Dell", 1, BigDecimal.valueOf(2500.00));
        order.addItem(item);
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Verify save
        assertThat(savedOrder.getId()).isEqualTo("test-order-1");
        assertThat(savedOrder.getCustomerName()).isEqualTo("João Silva");
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getItems()).hasSize(1);
        
        // Retrieve order
        Optional<Order> retrievedOrder = orderRepository.findById("test-order-1");
        assertThat(retrievedOrder).isPresent();
        assertThat(retrievedOrder.get().getCustomerName()).isEqualTo("João Silva");
        assertThat(retrievedOrder.get().getItems()).hasSize(1);
        assertThat(retrievedOrder.get().getItems().get(0).getProductName()).isEqualTo("Notebook Dell");
    }

    @Test
    public void testFindByCustomerId() {
        // Create multiple orders for same customer
        Order order1 = new Order("order-1", "CUST-123", "João Silva", BigDecimal.valueOf(1000.00));
        Order order2 = new Order("order-2", "CUST-123", "João Silva", BigDecimal.valueOf(2000.00));
        Order order3 = new Order("order-3", "CUST-456", "Maria Santos", BigDecimal.valueOf(1500.00));
        
        orderRepository.saveAll(List.of(order1, order2, order3));
        
        // Find orders by customer
        List<Order> customerOrders = orderRepository.findByCustomerId("CUST-123");
        
        assertThat(customerOrders).hasSize(2);
        assertThat(customerOrders).extracting(Order::getCustomerName).containsOnly("João Silva");
        assertThat(customerOrders).extracting(Order::getId).containsExactlyInAnyOrder("order-1", "order-2");
    }

    @Test
    public void testFindByStatus() {
        // Create orders with different statuses
        Order order1 = new Order("order-1", "CUST-123", "João Silva", BigDecimal.valueOf(1000.00));
        order1.setStatus(OrderStatus.CONFIRMED);
        
        Order order2 = new Order("order-2", "CUST-456", "Maria Santos", BigDecimal.valueOf(2000.00));
        order2.setStatus(OrderStatus.PENDING);
        
        Order order3 = new Order("order-3", "CUST-789", "Pedro Costa", BigDecimal.valueOf(1500.00));
        order3.setStatus(OrderStatus.CONFIRMED);
        
        orderRepository.saveAll(List.of(order1, order2, order3));
        
        // Find confirmed orders
        List<Order> confirmedOrders = orderRepository.findByStatus(OrderStatus.CONFIRMED);
        
        assertThat(confirmedOrders).hasSize(2);
        assertThat(confirmedOrders).extracting(Order::getId).containsExactlyInAnyOrder("order-1", "order-3");
        assertThat(confirmedOrders).allMatch(order -> order.getStatus() == OrderStatus.CONFIRMED);
    }

    @Test
    public void testCountByStatus() {
        // Create orders with different statuses
        Order order1 = new Order("order-1", "CUST-123", "João Silva", BigDecimal.valueOf(1000.00));
        order1.setStatus(OrderStatus.CONFIRMED);
        
        Order order2 = new Order("order-2", "CUST-456", "Maria Santos", BigDecimal.valueOf(2000.00));
        order2.setStatus(OrderStatus.CONFIRMED);
        
        Order order3 = new Order("order-3", "CUST-789", "Pedro Costa", BigDecimal.valueOf(1500.00));
        order3.setStatus(OrderStatus.PENDING);
        
        orderRepository.saveAll(List.of(order1, order2, order3));
        
        // Count by status
        long confirmedCount = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long failedCount = orderRepository.countByStatus(OrderStatus.FAILED);
        
        assertThat(confirmedCount).isEqualTo(2);
        assertThat(pendingCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(0);
    }

    @Test
    public void testOrderItemRelationship() {
        // Create order with multiple items
        Order order = new Order("order-with-items", "CUST-123", "João Silva", BigDecimal.valueOf(3500.00));
        
        OrderItemEntity item1 = new OrderItemEntity("PROD-1", "Notebook", 1, BigDecimal.valueOf(2500.00));
        OrderItemEntity item2 = new OrderItemEntity("PROD-2", "Mouse", 2, BigDecimal.valueOf(500.00));
        
        order.addItem(item1);
        order.addItem(item2);
        
        // Save order (should cascade to items)
        Order savedOrder = orderRepository.save(order);
        
        // Verify items are saved and relationships are correct
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getItems()).extracting(OrderItemEntity::getProductName)
                .containsExactlyInAnyOrder("Notebook", "Mouse");
        
        // Verify bidirectional relationship
        savedOrder.getItems().forEach(item -> {
            assertThat(item.getOrder()).isEqualTo(savedOrder);
            assertThat(item.getId()).isNotNull(); // Auto-generated ID
        });
        
        // Retrieve and verify persistence
        Optional<Order> retrievedOrder = orderRepository.findById("order-with-items");
        assertThat(retrievedOrder).isPresent();
        assertThat(retrievedOrder.get().getItems()).hasSize(2);
    }
}