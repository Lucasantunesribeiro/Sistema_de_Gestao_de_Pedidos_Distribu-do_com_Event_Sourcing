package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEvent {
    
    @JsonProperty("eventId")
    private final String eventId;
    
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;
    
    @JsonProperty("correlationId")
    private final String correlationId;
    
    @JsonProperty("causationId")
    private final String causationId;
    
    @JsonProperty("eventType")
    private final String eventType;
    
    @JsonProperty("version")
    private final int version;

    protected BaseEvent(String correlationId, String causationId, String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.causationId = causationId;
        this.eventType = eventType;
        this.version = 1;
    }

    protected BaseEvent(String eventId, LocalDateTime timestamp, String correlationId, 
                       String causationId, String eventType, int version) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.eventType = eventType;
        this.version = version;
    }

    // Getters
    public String getEventId() { return eventId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getCausationId() { return causationId; }
    public String getEventType() { return eventType; }
    public int getVersion() { return version; }
}