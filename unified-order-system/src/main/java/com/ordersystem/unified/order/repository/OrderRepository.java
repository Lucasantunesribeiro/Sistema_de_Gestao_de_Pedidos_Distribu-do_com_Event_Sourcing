package com.ordersystem.unified.order.repository;

import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Find orders by customer ID.
     */
    List<Order> findByCustomerId(String customerId);

    /**
     * Find orders by status.
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders by customer ID and status.
     */
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);

    /**
     * Find orders created within a date range.
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find orders by correlation ID for tracing.
     */
    Optional<Order> findByCorrelationId(String correlationId);

    /**
     * Count orders by status.
     */
    long countByStatus(OrderStatus status);

    /**
     * Find recent orders (last N orders).
     */
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(org.springframework.data.domain.Pageable pageable);

    /**
     * Find orders that are not in terminal status (for monitoring).
     */
    @Query("SELECT o FROM Order o WHERE o.status NOT IN ('CONFIRMED', 'CANCELLED', 'FAILED')")
    List<Order> findNonTerminalOrders();

    /**
     * Check if an order exists by ID.
     */
    boolean existsById(String id);
}