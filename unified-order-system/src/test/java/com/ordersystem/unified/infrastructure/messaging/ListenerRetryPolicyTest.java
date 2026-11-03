package com.ordersystem.unified.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.ordersystem.common.messaging.MessagingAutoConfiguration;
import com.ordersystem.unified.query.projection.OutOfOrderEventException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.retry.support.RetryTemplate;

/**
 * Política de retry da factory compartilhada de listeners: falha transitória é tentada de novo;
 * mensagem rejeitada explicitamente (malformada) não é, e segue direto para a DLQ.
 */
class ListenerRetryPolicyTest {

    private final Message message = new Message(new byte[0], new MessageProperties());

    @Test
    void transientFailureIsRetriedUpToMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> template().execute(context -> {
            attempts.incrementAndGet();
            throw new ListenerExecutionFailedException("listener failed",
                new OutOfOrderEventException("gap"), message);
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(attempts.get()).isEqualTo(MessagingAutoConfiguration.LISTENER_MAX_ATTEMPTS);
    }

    @Test
    void rejectedMessageIsNotRetriedEvenWhenWrappedByTheContainer() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> template().execute(context -> {
            attempts.incrementAndGet();
            throw new ListenerExecutionFailedException("listener failed",
                new AmqpRejectAndDontRequeueException("malformed"), message);
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void sharedListenerFactoryBuildsWithThePolicy() {
        SimpleRabbitListenerContainerFactory factory = new MessagingAutoConfiguration()
            .rabbitListenerContainerFactory(mock(ConnectionFactory.class), new Jackson2JsonMessageConverter());

        assertThat(factory.getAdviceChain()).hasSize(1);
    }

    private static RetryTemplate template() {
        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(MessagingAutoConfiguration.listenerRetryPolicy());
        return template;
    }
}
