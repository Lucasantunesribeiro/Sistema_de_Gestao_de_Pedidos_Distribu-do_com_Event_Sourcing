package com.ordersystem.unified.order.domain;

import com.ordersystem.unified.domain.events.BaseEvent;
import java.util.Objects;

/**
 * Evento já persistido no stream do pedido, com a versão (sequência) que recebeu no event store.
 */
public record RecordedOrderEvent(long version, BaseEvent event) {

    public RecordedOrderEvent {
        Objects.requireNonNull(event, "event");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }
}
