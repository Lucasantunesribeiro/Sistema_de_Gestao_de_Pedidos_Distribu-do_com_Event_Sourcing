package com.ordersystem.unified.query.projection;

import static org.assertj.core.api.Assertions.assertThat;

import com.ordersystem.common.messaging.MessagingConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

class OrderProjectionMessagingConfigTest {

    private final Declarables topology = new OrderProjectionMessagingConfig().orderProjectionTopology();

    @Test
    void projectionQueueDeadLettersToItsOwnDlq() {
        Queue queue = queue(MessagingConstants.ORDER_PROJECTION_QUEUE);

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
            .containsEntry("x-dead-letter-exchange", MessagingConstants.ORDER_DLX_EXCHANGE)
            .containsEntry("x-dead-letter-routing-key", MessagingConstants.ORDER_PROJECTION_DLQ);
        assertThat(queue(MessagingConstants.ORDER_PROJECTION_DLQ).isDurable()).isTrue();
    }

    @Test
    void bindingsRouteOrderEventsAndDeadLetters() {
        assertThat(topology.getDeclarablesByType(Binding.class))
            .extracting(Binding::getDestination, Binding::getExchange, Binding::getRoutingKey)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(MessagingConstants.ORDER_PROJECTION_QUEUE,
                    MessagingConstants.ORDER_EXCHANGE, "order.#"),
                org.assertj.core.groups.Tuple.tuple(MessagingConstants.ORDER_PROJECTION_DLQ,
                    MessagingConstants.ORDER_DLX_EXCHANGE, MessagingConstants.ORDER_PROJECTION_DLQ));
    }

    private Queue queue(String name) {
        return topology.getDeclarablesByType(Queue.class).stream()
            .filter(candidate -> candidate.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }
}
