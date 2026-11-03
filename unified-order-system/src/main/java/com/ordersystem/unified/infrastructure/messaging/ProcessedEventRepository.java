package com.ordersystem.unified.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    long countByIdConsumer(String consumer);

    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.id.consumer = :consumer")
    int deleteByConsumer(@Param("consumer") String consumer);
}
