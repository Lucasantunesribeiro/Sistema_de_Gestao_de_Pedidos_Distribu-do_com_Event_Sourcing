package com.ordersystem.inventory.config;

import com.ordersystem.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public FanoutExchange orderFanoutExchange() {
        return ExchangeBuilder.fanoutExchange(MessagingConstants.ORDER_FANOUT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(MessagingConstants.INVENTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue inventoryReservationQueue() {
        return QueueBuilder.durable(MessagingConstants.INVENTORY_RESERVATION_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue inventoryConfirmationQueue() {
        return QueueBuilder.durable(MessagingConstants.INVENTORY_CONFIRMATION_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue inventoryReleaseQueue() {
        return QueueBuilder.durable(MessagingConstants.INVENTORY_RELEASE_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(MessagingConstants.ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, FanoutExchange orderFanoutExchange) {
        return BindingBuilder.bind(inventoryQueue).to(orderFanoutExchange);
    }

    @Bean
    public DirectExchange inventoryDeadLetterExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.INVENTORY_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue inventoryDeadLetterQueue() {
        return QueueBuilder.durable(MessagingConstants.INVENTORY_DLQ_QUEUE).build();
    }

    @Bean
    public Binding inventoryDeadLetterBinding(Queue inventoryDeadLetterQueue, DirectExchange inventoryDeadLetterExchange) {
        return BindingBuilder.bind(inventoryDeadLetterQueue)
                .to(inventoryDeadLetterExchange)
                .with(MessagingConstants.DLQ_ROUTING_KEY);
    }
}
