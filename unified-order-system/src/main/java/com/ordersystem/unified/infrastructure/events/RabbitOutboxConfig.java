package com.ordersystem.unified.infrastructure.events;

import com.ordersystem.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.messaging.outbox", name = "enabled", havingValue = "true")
public class RabbitOutboxConfig {

    @Bean
    public Declarables outboxBrokerTopology() {
        TopicExchange orderExchange = new TopicExchange(MessagingConstants.ORDER_EXCHANGE, true, false);
        TopicExchange paymentExchange = new TopicExchange(MessagingConstants.PAYMENT_EXCHANGE, true, false);
        TopicExchange inventoryExchange = new TopicExchange(MessagingConstants.INVENTORY_EXCHANGE, true, false);

        Queue orderAuditQueue = new Queue(MessagingConstants.ORDER_AUDIT_QUEUE, true);
        Queue paymentAuditQueue = new Queue(MessagingConstants.PAYMENT_AUDIT_QUEUE, true);
        Queue inventoryAuditQueue = new Queue(MessagingConstants.INVENTORY_AUDIT_QUEUE, true);

        Binding orderBinding = BindingBuilder.bind(orderAuditQueue).to(orderExchange).with("order.#");
        Binding paymentBinding = BindingBuilder.bind(paymentAuditQueue).to(paymentExchange).with("payment.#");
        Binding inventoryBinding = BindingBuilder.bind(inventoryAuditQueue).to(inventoryExchange).with("inventory.#");

        return new Declarables(
            orderExchange,
            paymentExchange,
            inventoryExchange,
            orderAuditQueue,
            paymentAuditQueue,
            inventoryAuditQueue,
            orderBinding,
            paymentBinding,
            inventoryBinding
        );
    }
}
