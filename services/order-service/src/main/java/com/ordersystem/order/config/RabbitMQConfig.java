package com.ordersystem.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
    public static final String ORDER_UPDATED_QUEUE = "order.updated.queue";
    public static final String PAYMENT_PROCESSING_QUEUE = "payment.processing.queue";
    public static final String INVENTORY_RESERVATION_QUEUE = "inventory.reservation.queue";
    
    // Routing keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";
    public static final String PAYMENT_PROCESSING_ROUTING_KEY = "payment.processing";
    public static final String INVENTORY_RESERVATION_ROUTING_KEY = "inventory.reservation";
    
    // Exchanges
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }
    
    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE);
    }
    
    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(INVENTORY_EXCHANGE);
    }
    
    // Queues
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE).build();
    }
    
    @Bean
    public Queue orderUpdatedQueue() {
        return QueueBuilder.durable(ORDER_UPDATED_QUEUE).build();
    }
    
    @Bean
    public Queue paymentProcessingQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSING_QUEUE).build();
    }
    
    @Bean
    public Queue inventoryReservationQueue() {
        return QueueBuilder.durable(INVENTORY_RESERVATION_QUEUE).build();
    }
    
    // Bindings
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }
    
    @Bean
    public Binding orderUpdatedBinding() {
        return BindingBuilder
                .bind(orderUpdatedQueue())
                .to(orderExchange())
                .with(ORDER_UPDATED_ROUTING_KEY);
    }
    
    @Bean
    public Binding paymentProcessingBinding() {
        return BindingBuilder
                .bind(paymentProcessingQueue())
                .to(paymentExchange())
                .with(PAYMENT_PROCESSING_ROUTING_KEY);
    }
    
    @Bean
    public Binding inventoryReservationBinding() {
        return BindingBuilder
                .bind(inventoryReservationQueue())
                .to(inventoryExchange())
                .with(INVENTORY_RESERVATION_ROUTING_KEY);
    }
    
    // Message converter
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}