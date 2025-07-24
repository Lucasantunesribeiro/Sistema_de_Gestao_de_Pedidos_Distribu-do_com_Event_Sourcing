package com.ordersystem.order.repository;

import com.ordersystem.order.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    
    List<OrderEvent> findByOrderIdOrderByVersionAsc(String orderId);
    
    @Query("SELECT MAX(e.version) FROM OrderEvent e WHERE e.orderId = :orderId")
    Optional<Long> findMaxVersionByOrderId(@Param("orderId") String orderId);
    
    boolean existsByOrderIdAndVersion(String orderId, Long version);
}