package com.ordersystem.unified.query.projection;

import com.ordersystem.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do consumidor da projeção: fila durável ligada a {@code order.exchange} (order.#) com
 * dead-letter para {@code order.dlx} -> {@code order.projection.dlq}.
 *
 * <p>Fila nova em vez de alterar as filas de auditoria existentes: mudar argumentos (x-dead-letter-*)
 * de uma fila já declarada no broker causa PRECONDITION_FAILED na redeclaração.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.messaging.consumers.order-projection", name = "enabled", havingValue = "true")
public class OrderProjectionMessagingConfig {

    @Bean
    public Declarables orderProjectionTopology() {
        TopicExchange orderExchange = new TopicExchange(MessagingConstants.ORDER_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(MessagingConstants.ORDER_DLX_EXCHANGE, true, false);

        Queue projectionQueue = QueueBuilder.durable(MessagingConstants.ORDER_PROJECTION_QUEUE)
            .deadLetterExchange(MessagingConstants.ORDER_DLX_EXCHANGE)
            .deadLetterRoutingKey(MessagingConstants.ORDER_PROJECTION_DLQ)
            .build();
        Queue deadLetterQueue = QueueBuilder.durable(MessagingConstants.ORDER_PROJECTION_DLQ).build();

        Binding projectionBinding = BindingBuilder.bind(projectionQueue).to(orderExchange).with("order.#");
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange)
            .with(MessagingConstants.ORDER_PROJECTION_DLQ);

        return new Declarables(orderExchange, deadLetterExchange, projectionQueue, deadLetterQueue,
            projectionBinding, deadLetterBinding);
    }
}
