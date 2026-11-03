package com.ordersystem.unified.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DomainEventOutboxPublisherTest {

    @Mock
    private DomainEventRepository domainEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DomainEventOutboxPublisher domainEventOutboxPublisher;

    @Test
    void shouldPublishPendingEventAndMarkItProcessed() {
        OutboxMessagingProperties properties = new OutboxMessagingProperties();
        properties.setBatchSize(10);
        properties.setEnabled(true);

        domainEventOutboxPublisher = new DomainEventOutboxPublisher(
            domainEventRepository,
            rabbitTemplate,
            objectMapper,
            properties
        );

        DomainEventEntity event = DomainEventEntity.builder()
            .aggregateId("order-123")
            .aggregateType("Order")
            .eventType("OrderCreatedEvent")
            .eventData("{\"orderId\":\"order-123\",\"status\":\"CONFIRMED\"}")
            .correlationId("corr-123")
            .build();
        event.setCreatedAt(LocalDateTime.now());

        when(domainEventRepository.findPendingForDispatch(any(Pageable.class))).thenReturn(List.of(event));
        when(domainEventRepository.save(ArgumentMatchers.any(DomainEventEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(rabbitTemplate)
            .convertAndSend(
                any(String.class),
                any(String.class),
                any(Object.class),
                any(MessagePostProcessor.class)
            );

        domainEventOutboxPublisher.publishPendingEvents();

        assertThat(event.isProcessed()).isTrue();
        verify(rabbitTemplate).convertAndSend(
            any(String.class),
            any(String.class),
            any(Object.class),
            any(MessagePostProcessor.class)
        );
        verify(domainEventRepository).save(event);
    }

    @Test
    void shouldSendStreamMetadataUsedByIdempotentConsumers() {
        OutboxMessagingProperties properties = new OutboxMessagingProperties();
        domainEventOutboxPublisher = new DomainEventOutboxPublisher(
            domainEventRepository, rabbitTemplate, objectMapper, properties);

        DomainEventEntity event = DomainEventEntity.builder()
            .aggregateId("order-9")
            .aggregateType("Order")
            .aggregateVersion(3)
            .eventType("OrderStatusUpdatedEvent")
            .eventData("{\"orderId\":\"order-9\",\"newStatus\":\"CONFIRMED\"}")
            .build();
        event.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5));

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        domainEventOutboxPublisher.dispatchEvent(event);
        verify(rabbitTemplate).convertAndSend(
            org.mockito.ArgumentMatchers.eq("order.exchange"),
            org.mockito.ArgumentMatchers.eq("order.status.updated"),
            any(Object.class),
            postProcessor.capture());

        Message message = postProcessor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        MessageProperties sent = message.getMessageProperties();
        assertThat(sent.getMessageId()).isEqualTo(event.getId());
        assertThat((Object) sent.getHeader("aggregateVersion")).isEqualTo(3L);
        assertThat((Object) sent.getHeader("aggregateType")).isEqualTo("Order");
        assertThat((Object) sent.getHeader("occurredAt")).isEqualTo("2026-01-02T03:04:05");
    }

    @Test
    void failedEventHoldsBackLaterVersionsOfTheSameStreamButNotOtherStreams() {
        domainEventOutboxPublisher = new DomainEventOutboxPublisher(
            domainEventRepository, rabbitTemplate, objectMapper, new OutboxMessagingProperties());

        DomainEventEntity orderAv1 = orderEvent("order-A", 1, "OrderCreatedEvent");
        DomainEventEntity orderAv2 = orderEvent("order-A", 2, "OrderStatusUpdatedEvent");
        DomainEventEntity orderBv1 = orderEvent("order-B", 1, "OrderCreatedEvent");

        when(domainEventRepository.findPendingForDispatch(any(Pageable.class)))
            .thenReturn(List.of(orderAv1, orderBv1, orderAv2));
        when(domainEventRepository.save(any(DomainEventEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        // Broker falha só no envio de order-A v1
        doAnswer(invocation -> {
            MessagePostProcessor processor = invocation.getArgument(3);
            Message message = processor.postProcessMessage(new Message(new byte[0], new MessageProperties()));
            if (orderAv1.getId().equals(message.getMessageProperties().getMessageId())) {
                throw new org.springframework.amqp.AmqpConnectException(new java.io.IOException("broker down"));
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(
            any(String.class), any(String.class), any(Object.class), any(MessagePostProcessor.class));

        domainEventOutboxPublisher.publishPendingEvents();

        assertThat(orderAv1.isProcessed()).isFalse();
        // v2 não pode sair antes de v1: o consumidor veria uma lacuna
        assertThat(orderAv2.isProcessed()).isFalse();
        // outro stream segue normalmente
        assertThat(orderBv1.isProcessed()).isTrue();
        verify(rabbitTemplate, org.mockito.Mockito.times(2)).convertAndSend(
            any(String.class), any(String.class), any(Object.class), any(MessagePostProcessor.class));
    }

    private static DomainEventEntity orderEvent(String orderId, long version, String type) {
        DomainEventEntity event = DomainEventEntity.builder()
            .aggregateId(orderId)
            .aggregateType("Order")
            .aggregateVersion(version)
            .eventType(type)
            .eventData("{\"orderId\":\"" + orderId + "\"}")
            .build();
        event.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(version));
        return event;
    }
}
