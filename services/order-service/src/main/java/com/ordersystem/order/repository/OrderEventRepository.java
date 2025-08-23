package com.ordersystem.order.repository;

import com.ordersystem.order.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, String> {
    
    List<OrderEvent> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);
    
    @Query("SELECT e FROM OrderEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.occurredAt ASC")
    List<OrderEvent> findEventsByAggregateId(@Param("aggregateId") String aggregateId);
    
    @Query("SELECT e FROM OrderEvent e WHERE e.eventType = :eventType ORDER BY e.occurredAt DESC")
    List<OrderEvent> findByEventTypeOrderByOccurredAtDesc(@Param("eventType") String eventType);
    
    @Query("SELECT COUNT(e) FROM OrderEvent e WHERE e.aggregateId = :aggregateId")
    Long countByAggregateId(@Param("aggregateId") String aggregateId);
}