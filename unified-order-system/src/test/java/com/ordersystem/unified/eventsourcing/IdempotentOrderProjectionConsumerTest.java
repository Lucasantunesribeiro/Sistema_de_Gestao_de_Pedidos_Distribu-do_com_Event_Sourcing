package com.ordersystem.unified.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ordersystem.unified.domain.events.OrderItem;
import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.infrastructure.events.DomainEventEntity;
import com.ordersystem.unified.infrastructure.events.DomainEventRepository;
import com.ordersystem.unified.infrastructure.messaging.IdempotentMessageProcessor;
import com.ordersystem.unified.infrastructure.messaging.ProcessedEventRepository;
import com.ordersystem.unified.order.application.OrderEventSourcingRepository;
import com.ordersystem.unified.order.domain.OrderAggregate;
import com.ordersystem.unified.query.projection.OrderProjection;
import com.ordersystem.unified.query.projection.OrderProjectionMessageHandler;
import com.ordersystem.unified.query.projection.OrderProjectionRebuilder;
import com.ordersystem.unified.query.projection.OrderProjectionRepository;
import com.ordersystem.unified.query.projection.OutOfOrderEventException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Consumidor idempotente da projeção: mensagens no formato publicado pelo outbox
 * ({@code DomainEventOutboxPublisher}), PostgreSQL real, transações confirmadas de verdade.
 */
class IdempotentOrderProjectionConsumerTest extends EventSourcingJpaTestSupport {

    @Autowired
    private OrderProjectionMessageHandler handler;

    @Autowired
    private IdempotentMessageProcessor processor;

    @Autowired
    private OrderProjectionRebuilder rebuilder;

    @Autowired
    private OrderEventSourcingRepository orderEvents;

    @Autowired
    private OrderProjectionRepository projectionRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DomainEventRepository domainEventRepository;

    @BeforeEach
    void cleanUp() {
        projectionRepository.deleteAllInBatch();
        processedEventRepository.deleteAllInBatch();
        domainEventRepository.deleteAllInBatch();
    }

    @Test
    void sameMessageDeliveredTwiceHasOneEffect() {
        String orderId = storeOrder(OrderStatus.INVENTORY_RESERVED);
        List<Message> messages = outboxMessages(orderId);
        Message created = messages.get(0);

        assertThat(handler.handle(created)).isTrue();
        assertThat(handler.handle(created)).isFalse();

        OrderProjection projection = projectionRepository.findById(orderId).orElseThrow();
        assertThat(projection.getAppliedEvents()).isEqualTo(1);
        assertThat(projection.getLastEventVersion()).isEqualTo(1);
        assertThat(processedEventRepository.countByIdConsumer(OrderProjectionMessageHandler.CONSUMER)).isEqualTo(1);
    }

    @Test
    void wholeStreamRedeliveredIsAppliedExactlyOnce() {
        String orderId = storeOrder(OrderStatus.CONFIRMED);
        List<Message> messages = outboxMessages(orderId);

        messages.forEach(handler::handle);
        messages.forEach(handler::handle);

        OrderProjection projection = projectionRepository.findById(orderId).orElseThrow();
        assertThat(projection.getStatus()).isEqualTo("CONFIRMED");
        assertThat(projection.getAppliedEvents()).isEqualTo(4);
        assertThat(projection.getLastEventVersion()).isEqualTo(4);
        assertThat(processedEventRepository.countByIdConsumer(OrderProjectionMessageHandler.CONSUMER)).isEqualTo(4);
    }

    @Test
    void failedEffectRollsBackDedupMarkerSoRedeliveryIsApplied() {
        String orderId = storeOrder(OrderStatus.INVENTORY_RESERVED);
        List<Message> messages = outboxMessages(orderId);

        // v2 antes de v1: lacuna -> exceção -> rollback (marcador não fica gravado)
        assertThatThrownBy(() -> handler.handle(messages.get(1))).isInstanceOf(OutOfOrderEventException.class);
        assertThat(processedEventRepository.count()).isZero();
        assertThat(projectionRepository.findById(orderId)).isEmpty();

        assertThat(handler.handle(messages.get(0))).isTrue();
        assertThat(handler.handle(messages.get(1))).isTrue();
        assertThat(projectionRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo("INVENTORY_RESERVED");
    }

    @Test
    void concurrentDuplicateDeliveriesProduceOneEffect() throws Exception {
        AtomicInteger effects = new AtomicInteger();
        CountDownLatch insideEffect = new CountDownLatch(1);
        Runnable slowEffect = () -> {
            effects.incrementAndGet();
            insideEffect.countDown();
            sleep(500);
        };

        CompletableFuture<Object> first = CompletableFuture.supplyAsync(
            () -> processor.processOnce("test-consumer", "evt-1", slowEffect));
        assertThat(insideEffect.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<Object> second = CompletableFuture.supplyAsync(() -> {
            try {
                return processor.processOnce("test-consumer", "evt-1", effects::incrementAndGet);
            } catch (DataIntegrityViolationException duplicateKey) {
                return duplicateKey; // retry do container: na próxima tentativa vira duplicata
            }
        });

        assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(true);
        Object secondResult = second.get(10, TimeUnit.SECONDS);
        assertThat(secondResult).satisfiesAnyOf(
            result -> assertThat(result).isEqualTo(false),
            result -> assertThat(result).isInstanceOf(DataIntegrityViolationException.class));
        assertThat(processor.processOnce("test-consumer", "evt-1", effects::incrementAndGet)).isFalse();
        assertThat(effects.get()).isEqualTo(1);
    }

    @Test
    void malformedMessageIsRejectedWithoutRequeue() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("aggregateType", "Order");
        Message withoutId = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);

        assertThatThrownBy(() -> handler.handle(withoutId)).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void rebuildingProjectionByReplayIsDeterministic() {
        String confirmed = storeOrder(OrderStatus.CONFIRMED);
        String cancelled = storeOrder(OrderStatus.INVENTORY_RESERVED);
        OrderAggregate aggregate = orderEvents.load(cancelled);
        aggregate.cancel("cliente desistiu", "admin", "corr", null, null, true, false);
        orderEvents.save(aggregate);

        // projeção "suja" pelo consumidor ao vivo (só parte dos eventos)
        handler.handle(outboxMessages(confirmed).get(0));

        OrderProjectionRebuilder.RebuildResult firstRun = rebuilder.rebuildAll();
        List<OrderProjection> afterFirst = projectionRepository.findAllByOrderByOrderIdAsc();
        OrderProjectionRebuilder.RebuildResult secondRun = rebuilder.rebuildAll();
        List<OrderProjection> afterSecond = projectionRepository.findAllByOrderByOrderIdAsc();

        assertThat(firstRun).isEqualTo(secondRun);
        assertThat(firstRun.eventsReplayed()).isEqualTo(4 + 3);
        assertThat(afterSecond).usingRecursiveFieldByFieldElementComparator().isEqualTo(afterFirst);
        assertThat(afterSecond).extracting(OrderProjection::getOrderId, OrderProjection::getStatus)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(confirmed, "CONFIRMED"),
                org.assertj.core.groups.Tuple.tuple(cancelled, "CANCELLED"));
    }

    private String storeOrder(OrderStatus target) {
        String orderId = "proj-" + java.util.UUID.randomUUID();
        OrderAggregate aggregate = OrderAggregate.create(orderId, "customer-1", "Maria",
            List.of(new OrderItem("p-1", "Produto", 1, new BigDecimal("42.50"))),
            new BigDecimal("42.50"), "corr");
        List<OrderStatus> path = List.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.PAYMENT_PROCESSING,
            OrderStatus.CONFIRMED);
        for (OrderStatus status : path) {
            if (path.indexOf(status) > path.indexOf(target)) {
                break;
            }
            aggregate.changeStatus(status, "step", "corr");
        }
        orderEvents.save(aggregate);
        return orderId;
    }

    /** Mesmo formato de {@code DomainEventOutboxPublisher}: messageId = id da linha, headers de stream. */
    private List<Message> outboxMessages(String orderId) {
        List<Message> messages = new ArrayList<>();
        for (DomainEventEntity event :
            domainEventRepository.findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc("Order", orderId)) {
            MessageProperties properties = new MessageProperties();
            properties.setMessageId(event.getId());
            properties.setType(event.getEventType());
            properties.setHeader("aggregateId", event.getAggregateId());
            properties.setHeader("aggregateType", event.getAggregateType());
            properties.setHeader("eventType", event.getEventType());
            properties.setHeader("eventId", event.getId());
            properties.setHeader("aggregateVersion", event.getAggregateVersion());
            properties.setHeader("occurredAt", event.getCreatedAt().toString());
            messages.add(new Message(event.getEventData().getBytes(StandardCharsets.UTF_8), properties));
        }
        return messages;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
