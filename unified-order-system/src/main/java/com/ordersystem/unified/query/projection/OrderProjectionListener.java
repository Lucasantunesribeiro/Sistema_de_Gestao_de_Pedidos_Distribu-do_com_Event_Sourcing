package com.ordersystem.unified.query.projection;

import com.ordersystem.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumidor RabbitMQ da projeção de pedidos.
 *
 * <p>Ack: container em modo AUTO; o ack só sai depois que {@code handle} retorna, isto é, depois do
 * commit da transação que gravou projeção + marcador em {@code processed_events}. Falha: retry
 * stateless (3 tentativas, backoff exponencial, ver {@code MessagingAutoConfiguration}) e depois
 * reject sem requeue, que manda a mensagem para {@code order.projection.dlq} via {@code order.dlx}.
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging.consumers.order-projection", name = "enabled", havingValue = "true")
public class OrderProjectionListener {

    private final OrderProjectionMessageHandler handler;

    public OrderProjectionListener(OrderProjectionMessageHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_PROJECTION_QUEUE)
    public void onOrderEvent(Message message) {
        handler.handle(message);
    }
}
