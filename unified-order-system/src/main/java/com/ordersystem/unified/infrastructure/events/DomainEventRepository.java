package com.ordersystem.unified.infrastructure.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for domain events (Event Sourcing).
 * Provides query methods for event retrieval and replay.
 */
@Repository
public interface DomainEventRepository extends JpaRepository<DomainEventEntity, String> {

    /**
     * Find all events for specific aggregate (for event replay).
     */
    List<DomainEventEntity> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);

    /**
     * Find events by aggregate type.
     */
    List<DomainEventEntity> findByAggregateTypeOrderByCreatedAtDesc(String aggregateType);

    /**
     * Find events by correlation ID (trace related events).
     */
    List<DomainEventEntity> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    /**
     * Find events by type.
     */
    List<DomainEventEntity> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * Find unprocessed events.
     */
    List<DomainEventEntity> findByProcessedFalseOrderByCreatedAtAsc();

    /**
     * Find a limited batch of unprocessed events for broker dispatch.
     */
    @Query("SELECT e FROM DomainEventEntity e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<DomainEventEntity> findPendingForDispatch(Pageable pageable);

    /**
     * Find events created within time range.
     */
    List<DomainEventEntity> findByCreatedAtBetweenOrderByCreatedAtAsc(
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * Find events for specific aggregate and type.
     */
    List<DomainEventEntity> findByAggregateIdAndEventTypeOrderByCreatedAtAsc(
        String aggregateId,
        String eventType
    );

    /**
     * Count events by aggregate ID.
     */
    long countByAggregateId(String aggregateId);

    /**
     * Count events by type.
     */
    long countByEventType(String eventType);

    /**
     * Count unprocessed events.
     */
    long countByProcessedFalse();

    /**
     * Find recent events (for monitoring).
     */
    @Query("SELECT e FROM DomainEventEntity e ORDER BY e.createdAt DESC LIMIT :limit")
    List<DomainEventEntity> findRecentEvents(@Param("limit") int limit);

    /**
     * Find events for user (audit trail).
     */
    List<DomainEventEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
