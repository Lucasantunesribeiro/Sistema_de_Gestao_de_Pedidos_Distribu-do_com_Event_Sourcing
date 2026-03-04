package com.ordersystem.query.config;

import com.ordersystem.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.PAYMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange inventoryExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.INVENTORY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(MessagingConstants.ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.QUERY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderUpdatedQueue() {
        return QueueBuilder.durable(MessagingConstants.ORDER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.QUERY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentProcessingQueue() {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.QUERY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue inventoryReservationQueue() {
        return QueueBuilder.durable(MessagingConstants.INVENTORY_RESERVATION_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.QUERY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(orderExchange)
                .with(MessagingConstants.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderUpdatedBinding(Queue orderUpdatedQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderUpdatedQueue)
                .to(orderExchange)
                .with(MessagingConstants.ORDER_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentProcessingBinding(Queue paymentProcessingQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentProcessingQueue)
                .to(paymentExchange)
                .with(MessagingConstants.PAYMENT_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryReservationBinding(Queue inventoryReservationQueue, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(inventoryReservationQueue)
                .to(inventoryExchange)
                .with(MessagingConstants.INVENTORY_RESERVATION_ROUTING_KEY);
    }

    @Bean
    public DirectExchange queryDeadLetterExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.QUERY_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue queryDeadLetterQueue() {
        return QueueBuilder.durable(MessagingConstants.QUERY_DLQ_QUEUE).build();
    }

    @Bean
    public Binding queryDeadLetterBinding(Queue queryDeadLetterQueue, DirectExchange queryDeadLetterExchange) {
        return BindingBuilder.bind(queryDeadLetterQueue)
                .to(queryDeadLetterExchange)
                .with(MessagingConstants.DLQ_ROUTING_KEY);
    }
}
