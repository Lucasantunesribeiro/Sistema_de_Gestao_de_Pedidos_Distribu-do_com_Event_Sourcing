package com.ordersystem.unified.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ordersystem.unified.domain.events.OrderItem;
import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.domain.events.PaymentProcessedEvent;
import com.ordersystem.unified.infrastructure.events.DomainEventEntity;
import com.ordersystem.unified.infrastructure.events.DomainEventRepository;
import com.ordersystem.unified.infrastructure.events.EventStore;
import com.ordersystem.unified.order.application.OrderEventSourcingRepository;
import com.ordersystem.unified.order.domain.OrderAggregate;
import com.ordersystem.unified.shared.exceptions.ConcurrencyException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class EventStoreConcurrencyTest extends EventSourcingJpaTestSupport {

    @Autowired
    private OrderEventSourcingRepository orderEvents;

    @Autowired
    private EventStore eventStore;

    @Autowired
    private DomainEventRepository domainEventRepository;

    private String orderId;

    @BeforeEach
    void setUp() {
        orderId = "es-" + UUID.randomUUID();
    }

    private OrderAggregate newOrder() {
        return OrderAggregate.create(orderId, "customer-1", "Maria",
            List.of(new OrderItem("p-1", "Produto", 1, new BigDecimal("30.00"))),
            new BigDecimal("30.00"), "corr");
    }

    @Test
    void savedAggregateIsRebuiltFromStoreInVersionOrder() {
        OrderAggregate aggregate = newOrder();
        orderEvents.save(aggregate);
        aggregate.changeStatus(OrderStatus.INVENTORY_RESERVED, "reserved", "corr");
        aggregate.changeStatus(OrderStatus.PAYMENT_PROCESSING, "paid", "corr");
        orderEvents.save(aggregate);

        OrderAggregate loaded = orderEvents.load(orderId);

        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);
        assertThat(loaded.getVersion()).isEqualTo(3);
        assertThat(eventStore.readStream("Order", orderId))
            .extracting(DomainEventEntity::getAggregateVersion)
            .containsExactly(1L, 2L, 3L);
    }

    @Test
    void secondWriterWithStaleVersionGetsConflictInsteadOfOverwriting() {
        orderEvents.save(newOrder());
        OrderAggregate writerA = orderEvents.load(orderId);
        OrderAggregate writerB = orderEvents.load(orderId);

        writerA.changeStatus(OrderStatus.INVENTORY_RESERVED, "A reservou", "corr-a");
        orderEvents.save(writerA);

        writerB.cancel("B cancelou", "operador", "corr-b", null, null, false, false);
        assertThatThrownBy(() -> orderEvents.save(writerB))
            .isInstanceOf(ConcurrencyException.class)
            .hasMessageContaining("expected version 1, actual 2");

        OrderAggregate current = orderEvents.load(orderId);
        assertThat(current.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        assertThat(current.getVersion()).isEqualTo(2);
    }

    @Test
    void creatingTheSameOrderStreamTwiceConflicts() {
        orderEvents.save(newOrder());

        assertThatThrownBy(() -> orderEvents.save(newOrder()))
            .isInstanceOf(ConcurrencyException.class);
        assertThat(domainEventRepository.countByAggregateId(orderId)).isEqualTo(1);
    }

    @Test
    void uniqueConstraintRejectsDuplicateStreamPositionEvenWithoutVersionCheck() {
        DomainEventEntity first = DomainEventEntity.builder()
            .aggregateType("Order").aggregateId(orderId).aggregateVersion(1)
            .eventType("OrderCreatedEvent").eventData("{}").build();
        DomainEventEntity sameSlot = DomainEventEntity.builder()
            .aggregateType("Order").aggregateId(orderId).aggregateVersion(1)
            .eventType("OrderCreatedEvent").eventData("{}").build();

        domainEventRepository.saveAndFlush(first);

        assertThatThrownBy(() -> domainEventRepository.saveAndFlush(sameSlot))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void streamsAreSeparatedByAggregateType() {
        orderEvents.save(newOrder());

        long paymentVersion = eventStore.appendToStream("Payment", orderId, List.of(
            new PaymentProcessedEvent("pay-1", orderId, "customer-1", new BigDecimal("30.00"), "APPROVED",
                "tx-1")));

        assertThat(paymentVersion).isEqualTo(1);
        assertThat(orderEvents.load(orderId).getVersion()).isEqualTo(1);
    }
}
