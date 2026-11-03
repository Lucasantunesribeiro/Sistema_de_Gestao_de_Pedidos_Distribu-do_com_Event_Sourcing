package com.ordersystem.unified.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Chave de deduplicação: o mesmo evento pode ser processado uma vez por consumidor. */
@Embeddable
public class ProcessedEventId implements Serializable {

    @Column(name = "consumer", nullable = false, length = 100)
    private String consumer;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    protected ProcessedEventId() {
    }

    public ProcessedEventId(String consumer, String eventId) {
        this.consumer = consumer;
        this.eventId = eventId;
    }

    public String getConsumer() { return consumer; }
    public String getEventId() { return eventId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEventId other)) return false;
        return Objects.equals(consumer, other.consumer) && Objects.equals(eventId, other.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumer, eventId);
    }
}
