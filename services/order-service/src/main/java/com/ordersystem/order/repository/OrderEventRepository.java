package com.ordersystem.order.repository;

import com.ordersystem.order.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    @Query("SELECT e FROM OrderEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.version ASC")
    List<OrderEvent> findByAggregateIdOrderByVersionAsc(@Param("aggregateId") String aggregateId);

    @Query("SELECT e FROM OrderEvent e WHERE e.aggregateId = :aggregateId AND e.version > :fromVersion ORDER BY e.version ASC")
    List<OrderEvent> findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
            @Param("aggregateId") String aggregateId, 
            @Param("fromVersion") Integer fromVersion);

    @Query("SELECT MAX(e.version) FROM OrderEvent e WHERE e.aggregateId = :aggregateId")
    Integer findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);

    @Query("SELECT DISTINCT e.aggregateId FROM OrderEvent e")
    List<String> findAllAggregateIds();

    @Query("SELECT e FROM OrderEvent e WHERE e.correlationId = :correlationId ORDER BY e.timestamp ASC")
    List<OrderEvent> findByCorrelationIdOrderByTimestampAsc(@Param("correlationId") String correlationId);

    @Query("SELECT e FROM OrderEvent e WHERE e.eventType = :eventType ORDER BY e.timestamp DESC")
    List<OrderEvent> findByEventTypeOrderByTimestampDesc(@Param("eventType") String eventType);

    boolean existsByAggregateIdAndVersion(String aggregateId, Integer version);
}