package com.ordersystem.common.messaging;

import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.policy.SimpleRetryPolicy;

@AutoConfiguration(before = RabbitAutoConfiguration.class)
public class MessagingAutoConfiguration {

    public static final int LISTENER_MAX_ATTEMPTS = 3;

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setBeforePublishPostProcessors(this::applyCorrelationId);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // AUTO: o ack só é enviado quando o listener retorna sem exceção, ou seja, depois do commit
        // da transação do handler. Exceção -> retry (3x) -> reject sem requeue -> dead-letter da fila.
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(listenerRetryPolicy())
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    /**
     * Até {@value #LISTENER_MAX_ATTEMPTS} tentativas por mensagem, exceto quando o listener lança
     * {@link AmqpRejectAndDontRequeueException} (também como causa): mensagem malformada não melhora
     * com retry e vai direto para a dead-letter da fila.
     */
    public static SimpleRetryPolicy listenerRetryPolicy() {
        return new SimpleRetryPolicy(
                LISTENER_MAX_ATTEMPTS,
                Map.of(AmqpRejectAndDontRequeueException.class, false),
                true,
                true);
    }

    private Message applyCorrelationId(Message message) {
        MessageProperties properties = message.getMessageProperties();
        String correlationId = valueOrBlank(properties.getHeader(MessagingConstants.CORRELATION_ID_HEADER));
        if (correlationId == null) {
            correlationId = valueOrBlank(MDC.get("correlationId"));
        }
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        properties.setHeader(MessagingConstants.CORRELATION_ID_HEADER, correlationId);
        properties.setCorrelationId(correlationId);
        return message;
    }

    private String valueOrBlank(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
