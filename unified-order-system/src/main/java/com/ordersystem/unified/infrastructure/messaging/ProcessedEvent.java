package com.ordersystem.unified.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

/**
 * Registro de "evento já processado" por consumidor (tabela {@code processed_events}).
 * Implementa {@link Persistable} para que o {@code save} faça INSERT direto (sem SELECT/merge):
 * uma entrega duplicada concorrente esbarra na chave primária.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent implements Persistable<ProcessedEventId> {

    @EmbeddedId
    private ProcessedEventId id;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Transient
    private boolean isNew = true;

    protected ProcessedEvent() {
        this.isNew = false;
    }

    public ProcessedEvent(String consumer, String eventId, LocalDateTime processedAt) {
        this.id = new ProcessedEventId(consumer, eventId);
        this.processedAt = processedAt;
    }

    @Override
    public ProcessedEventId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
