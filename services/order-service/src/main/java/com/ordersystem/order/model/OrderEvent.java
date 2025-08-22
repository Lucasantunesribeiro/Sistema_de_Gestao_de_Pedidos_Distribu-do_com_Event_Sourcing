package com.ordersystem.order.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_events", indexes = {
    @Index(name = "idx_order_events_aggregate_id", columnList = "aggregateId"),
    @Index(name = "idx_order_events_timestamp", columnList = "timestamp"),
    @Index(name = "idx_order_events_type", columnList = "eventType"),
    @Index(name = "idx_order_events_correlation", columnList = "correlationId")
})
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "causation_id", length = 255)
    private String causationId;

    @Column(name = "user_id", length = 255)
    private String userId;

    // Unique constraint on aggregateId + version
    @Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"aggregate_id", "version"})
    })
    public static class UniqueConstraint {}

    // Default constructor
    public OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public OrderEvent(String aggregateId, String eventType, Object eventData, 
                     Integer version, String correlationId, String causationId, String userId) {
        this();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.setEventDataFromObject(eventData);
        this.version = version;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.userId = userId;
    }

    // Helper method to serialize event data
    public void setEventDataFromObject(Object eventData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            this.eventData = mapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }

    // Helper method to deserialize event data
    public <T> T getEventDataAsObject(Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(this.eventData, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize event data", e);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getCausationId() { return causationId; }
    public void setCausationId(String causationId) { this.causationId = causationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}