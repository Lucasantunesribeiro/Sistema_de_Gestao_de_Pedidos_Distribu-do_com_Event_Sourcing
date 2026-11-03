package com.ordersystem.unified.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ordersystem.unified.domain.events.BaseEvent;
import com.ordersystem.unified.domain.events.OrderCancelledEvent;
import com.ordersystem.unified.domain.events.OrderCreatedEvent;
import com.ordersystem.unified.domain.events.OrderItem;
import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.domain.events.OrderStatusUpdatedEvent;
import com.ordersystem.unified.shared.exceptions.EventStreamCorruptedException;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderAggregateTest {

    private static final String ORDER_ID = "order-1";

    private static OrderAggregate newOrder() {
        return OrderAggregate.create(ORDER_ID, "customer-1", "Maria",
            List.of(new OrderItem("p-1", "Produto", 2, new BigDecimal("25.00"))),
            new BigDecimal("50.00"), "corr-1");
    }

    /** Pedido percorrendo o fluxo feliz; devolve os eventos já versionados (1..4). */
    private static List<RecordedOrderEvent> confirmedHistory() {
        OrderAggregate aggregate = newOrder();
        aggregate.changeStatus(OrderStatus.INVENTORY_RESERVED, "reserved", "corr-1");
        aggregate.changeStatus(OrderStatus.PAYMENT_PROCESSING, "paid", "corr-1");
        aggregate.changeStatus(OrderStatus.CONFIRMED, "confirmed", "corr-1");
        return toRecorded(aggregate.getUncommittedEvents(), 1);
    }

    private static List<RecordedOrderEvent> toRecorded(List<BaseEvent> events, long firstVersion) {
        List<RecordedOrderEvent> recorded = new ArrayList<>();
        long version = firstVersion;
        for (BaseEvent event : events) {
            recorded.add(new RecordedOrderEvent(version++, event));
        }
        return recorded;
    }

    @Nested
    @DisplayName("comandos")
    class Commands {

        @Test
        void createRecordsOrderCreatedAtVersionOneWithNothingCommitted() {
            OrderAggregate aggregate = newOrder();

            assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(aggregate.getVersion()).isEqualTo(1);
            assertThat(aggregate.getCommittedVersion()).isZero();
            assertThat(aggregate.getUncommittedEvents()).singleElement().isInstanceOf(OrderCreatedEvent.class);
        }

        @Test
        void invalidTransitionIsRejectedAndRecordsNothing() {
            OrderAggregate aggregate = newOrder();

            assertThatThrownBy(() -> aggregate.changeStatus(OrderStatus.CONFIRMED, "skip", "corr"))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("PENDING -> CONFIRMED");
            assertThat(aggregate.getVersion()).isEqualTo(1);
            assertThat(aggregate.getUncommittedEvents()).hasSize(1);
        }

        @Test
        void terminalOrderCannotBeCancelledAgain() {
            OrderAggregate aggregate = newOrder();
            aggregate.cancel("cliente desistiu", "admin", "corr", null, null, false, false);

            assertThatThrownBy(aggregate::ensureCanBeCancelled).isInstanceOf(InvalidOrderException.class);
            assertThatThrownBy(() -> aggregate.cancel("de novo", "admin", "corr", null, null, false, false))
                .isInstanceOf(InvalidOrderException.class);
            assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(aggregate.getVersion()).isEqualTo(2);
        }

        @Test
        void failedOrderCannotMoveAnywhere() {
            OrderAggregate aggregate = newOrder();
            aggregate.changeStatus(OrderStatus.FAILED, "sem estoque", "corr");

            assertThatThrownBy(() -> aggregate.changeStatus(OrderStatus.INVENTORY_RESERVED, "x", "corr"))
                .isInstanceOf(InvalidOrderException.class);
        }

        @Test
        void confirmedOrderCanBeCancelled() {
            OrderAggregate aggregate = OrderAggregate.rehydrate(ORDER_ID, confirmedHistory());

            aggregate.cancel("estorno", "admin", "corr", "res-1", "pay-1", true, true);

            assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(aggregate.getCancellationReason()).isEqualTo("estorno");
            assertThat(aggregate.getCommittedVersion()).isEqualTo(4);
            assertThat(aggregate.getVersion()).isEqualTo(5);
        }

        @Test
        void commandsOnMissingStreamAreRejected() {
            OrderAggregate aggregate = OrderAggregate.empty(ORDER_ID);

            assertThat(aggregate.exists()).isFalse();
            assertThatThrownBy(() -> aggregate.changeStatus(OrderStatus.FAILED, "x", "corr"))
                .isInstanceOf(InvalidOrderException.class);
        }

        @Test
        void markCommittedMovesExpectedVersion() {
            OrderAggregate aggregate = newOrder();
            aggregate.changeStatus(OrderStatus.INVENTORY_RESERVED, "reserved", "corr");

            aggregate.markCommitted();

            assertThat(aggregate.getCommittedVersion()).isEqualTo(2);
            assertThat(aggregate.getUncommittedEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("replay")
    class Replay {

        @Test
        void rehydrateAppliesEventsInVersionOrderEvenIfLoadedShuffled() {
            List<RecordedOrderEvent> history = new ArrayList<>(confirmedHistory());
            Collections.reverse(history);

            OrderAggregate aggregate = OrderAggregate.rehydrate(ORDER_ID, history);

            assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(aggregate.getVersion()).isEqualTo(4);
            assertThat(aggregate.getCommittedVersion()).isEqualTo(4);
            assertThat(aggregate.getUncommittedEvents()).isEmpty();
            assertThat(aggregate.getTotalAmount()).isEqualByComparingTo("50.00");
            assertThat(aggregate.getItems()).hasSize(1);
        }

        @Test
        void replayingTheSameStreamTwiceGivesTheSameState() {
            List<RecordedOrderEvent> history = confirmedHistory();

            OrderAggregate first = OrderAggregate.rehydrate(ORDER_ID, history);
            OrderAggregate second = OrderAggregate.rehydrate(ORDER_ID, history);

            assertThat(second).usingRecursiveComparison().isEqualTo(first);
        }

        @Test
        void gapInVersionsIsCorruption() {
            List<RecordedOrderEvent> history = new ArrayList<>(confirmedHistory());
            history.remove(1); // v2 missing

            assertThatThrownBy(() -> OrderAggregate.rehydrate(ORDER_ID, history))
                .isInstanceOf(EventStreamCorruptedException.class)
                .hasMessageContaining("expected event version 2 but found 3");
        }

        @Test
        void duplicatedVersionIsCorruption() {
            List<RecordedOrderEvent> history = new ArrayList<>(confirmedHistory());
            history.add(new RecordedOrderEvent(4, history.get(3).event()));

            assertThatThrownBy(() -> OrderAggregate.rehydrate(ORDER_ID, history))
                .isInstanceOf(EventStreamCorruptedException.class);
        }

        @Test
        void streamMustStartWithOrderCreated() {
            List<RecordedOrderEvent> history = List.of(new RecordedOrderEvent(1,
                new OrderStatusUpdatedEvent(ORDER_ID, "c", "PENDING", "INVENTORY_RESERVED", "x")));

            assertThatThrownBy(() -> OrderAggregate.rehydrate(ORDER_ID, history))
                .isInstanceOf(EventStreamCorruptedException.class);
        }

        @Test
        void storedInvalidTransitionIsCorruption() {
            List<RecordedOrderEvent> history = new ArrayList<>(toRecorded(newOrder().getUncommittedEvents(), 1));
            history.add(new RecordedOrderEvent(2,
                new OrderStatusUpdatedEvent(ORDER_ID, "customer-1", "PENDING", "CONFIRMED", "pulou etapas")));

            assertThatThrownBy(() -> OrderAggregate.rehydrate(ORDER_ID, history))
                .isInstanceOf(EventStreamCorruptedException.class)
                .hasMessageContaining("v2");
        }

        @Test
        void eventWhoseOldStatusDisagreesWithStateIsCorruption() {
            List<RecordedOrderEvent> history = new ArrayList<>(toRecorded(newOrder().getUncommittedEvents(), 1));
            history.add(new RecordedOrderEvent(2,
                new OrderStatusUpdatedEvent(ORDER_ID, "customer-1", "INVENTORY_RESERVED", "PAYMENT_PROCESSING", "x")));

            assertThatThrownBy(() -> OrderAggregate.rehydrate(ORDER_ID, history))
                .isInstanceOf(EventStreamCorruptedException.class);
        }

        @Test
        void eventsSurviveJsonRoundTripLikeTheEventStore() throws Exception {
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            OrderAggregate original = OrderAggregate.rehydrate(ORDER_ID, confirmedHistory());
            original.cancel("estorno", "admin", "corr", "res-1", "pay-1", true, false);
            List<BaseEvent> all = new ArrayList<>();
            confirmedHistory().forEach(recorded -> all.add(recorded.event()));
            all.addAll(original.getUncommittedEvents());

            List<RecordedOrderEvent> fromJson = new ArrayList<>();
            long version = 1;
            for (BaseEvent event : all) {
                String json = mapper.writeValueAsString(event);
                fromJson.add(new RecordedOrderEvent(version++, mapper.readValue(json, event.getClass())));
            }

            OrderAggregate replayed = OrderAggregate.rehydrate(ORDER_ID, fromJson);
            assertThat(replayed.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(replayed.getVersion()).isEqualTo(5);
            assertThat(replayed.getCancellationReason()).isEqualTo("estorno");
            assertThat(fromJson.get(4).event()).isInstanceOf(OrderCancelledEvent.class);
            assertThat(fromJson.get(0).event().getEventId()).isEqualTo(all.get(0).getEventId());
        }
    }
}
