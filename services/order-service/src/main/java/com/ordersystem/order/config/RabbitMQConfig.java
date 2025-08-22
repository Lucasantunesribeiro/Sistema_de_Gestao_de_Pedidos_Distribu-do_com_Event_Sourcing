package com.ordersystem.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";

    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";
    public static final String INVENTORY_RESERVED_QUEUE = "inventory.reserved.queue";
    public static final String INVENTORY_RESERVATION_FAILED_QUEUE = "inventory.reservation.failed.queue";

    // Routing keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED_ROUTING_KEY = "inventory.reservation.failed";

    // Dead Letter Queue configuration
    public static final String DLQ_EXCHANGE = "dlq.exchange";
    public static final String ORDER_DLQ = "order.dlq";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message not delivered: " + cause);
            }
        });
        template.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage());
        });
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    // Exchanges
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return ExchangeBuilder.topicExchange(INVENTORY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlqExchange() {
        return ExchangeBuilder.directExchange(DLQ_EXCHANGE)
                .durable(true)
                .build();
    }

    // Queues
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .build();
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue inventoryReservedQueue() {
        return QueueBuilder.durable(INVENTORY_RESERVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue inventoryReservationFailedQueue() {
        return QueueBuilder.durable(INVENTORY_RESERVATION_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue orderDlq() {
        return QueueBuilder.durable(ORDER_DLQ).build();
    }

    // Bindings
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder.bind(paymentProcessedQueue())
                .to(paymentExchange())
                .with(PAYMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryReservedBinding() {
        return BindingBuilder.bind(inventoryReservedQueue())
                .to(inventoryExchange())
                .with(INVENTORY_RESERVED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryReservationFailedBinding() {
        return BindingBuilder.bind(inventoryReservationFailedQueue())
                .to(inventoryExchange())
                .with(INVENTORY_RESERVATION_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(orderDlq())
                .to(dlqExchange())
                .with(ORDER_DLQ);
    }
}