package com.ordersystem.payment.config;

import com.ordersystem.common.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(MessagingConstants.ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue paymentProcessingQueue() {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.PAYMENT_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentResultsQueue() {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_RESULTS_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.PAYMENT_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding paymentProcessingBinding(Queue paymentProcessingQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(paymentProcessingQueue)
                .to(orderExchange)
                .with(MessagingConstants.PAYMENT_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public Binding paymentProcessedBinding(Queue paymentResultsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(paymentResultsQueue)
                .to(orderExchange)
                .with(MessagingConstants.PAYMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentResultsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(paymentResultsQueue)
                .to(orderExchange)
                .with(MessagingConstants.PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange paymentDeadLetterExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.PAYMENT_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue paymentDeadLetterQueue() {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_DLQ_QUEUE).build();
    }

    @Bean
    public Binding paymentDeadLetterBinding(Queue paymentDeadLetterQueue, DirectExchange paymentDeadLetterExchange) {
        return BindingBuilder.bind(paymentDeadLetterQueue)
                .to(paymentDeadLetterExchange)
                .with(MessagingConstants.DLQ_ROUTING_KEY);
    }
}
