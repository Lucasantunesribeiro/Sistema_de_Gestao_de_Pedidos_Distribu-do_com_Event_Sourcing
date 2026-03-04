package com.ordersystem.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EventEnvelope<T extends VersionedEvent> {

    private final UUID eventId;
    private final String eventType;
    private final String aggregateId;
    private final OffsetDateTime occurredAt;
    private final String schemaVersion;
    private final String correlationId;
    private final String causationId;
    private final T payload;

    private EventEnvelope(Builder<T> builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.aggregateId = builder.aggregateId;
        this.occurredAt = builder.occurredAt;
        this.schemaVersion = builder.schemaVersion;
        this.correlationId = builder.correlationId;
        this.causationId = builder.causationId;
        this.payload = builder.payload;
    }

    public static <T extends VersionedEvent> Builder<T> builder(Class<T> payloadType) {
        return new Builder<>();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public T getPayload() {
        return payload;
    }

    public static final class Builder<T extends VersionedEvent> {
        private UUID eventId = UUID.randomUUID();
        private String eventType;
        private String aggregateId;
        private OffsetDateTime occurredAt = OffsetDateTime.now();
        private String schemaVersion = "v1";
        private String correlationId;
        private String causationId;
        private T payload;

        private Builder() {
        }

        public Builder<T> eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder<T> aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder<T> occurredAt(OffsetDateTime occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder<T> schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder<T> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<T> causationId(String causationId) {
            this.causationId = causationId;
            return this;
        }

        public Builder<T> payload(T payload) {
            this.payload = payload;
            return this;
        }

        public EventEnvelope<T> build() {
            Objects.requireNonNull(eventType, "eventType is required");
            Objects.requireNonNull(aggregateId, "aggregateId is required");
            Objects.requireNonNull(payload, "payload is required");
            return new EventEnvelope<>(this);
        }
    }
}
