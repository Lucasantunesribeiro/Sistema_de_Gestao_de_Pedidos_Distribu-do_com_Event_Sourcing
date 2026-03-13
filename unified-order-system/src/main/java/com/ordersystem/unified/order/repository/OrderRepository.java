package com.ordersystem.unified.order.repository;

import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.domain.events.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    List<Order> findByCustomerId(String customerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    Page<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable);
    
    // List variants for tests/simpler use cases
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);
    
    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    long countByStatus(OrderStatus status);

    Optional<Order> findByCorrelationId(String correlationId);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.status NOT IN ('CANCELLED', 'DELIVERED', 'RETURNED')")
    List<Order> findNonTerminalOrders();

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    List<Order> findOrdersCreatedBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
                                         @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
                                         
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);
}
