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
import org.springframework.amqp.core.MessagePostProcessor;
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
}
