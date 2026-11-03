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
}
