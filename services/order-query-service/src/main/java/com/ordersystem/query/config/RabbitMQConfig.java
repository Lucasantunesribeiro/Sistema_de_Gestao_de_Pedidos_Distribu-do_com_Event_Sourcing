package com.ordersystem.query.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Enhanced RabbitMQ configuration for Query Service with resilience patterns
 * Fixed to match Order Service exchange configuration
 */
@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    // Exchange names - matching Order Service
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";

    // Queue names - matching Order Service
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_UPDATED_QUEUE = "order.updated.queue";
    public static final String PAYMENT_PROCESSING_QUEUE = "payment.processing.queue";
    public static final String INVENTORY_RESERVATION_QUEUE = "inventory.reservation.queue";

    // Routing keys - matching Order Service
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";
    public static final String PAYMENT_PROCESSING_ROUTING_KEY = "payment.processing";
    public static final String INVENTORY_RESERVATION_ROUTING_KEY = "inventory.reservation";

    // Dead Letter Exchange
    public static final String DLX_EXCHANGE = "query.dlx";
    public static final String DLQ_QUEUE = "query.dlq";

    @Value("${RABBITMQ_HOST:localhost}")
    private String host;

    @Value("${RABBITMQ_PORT:5672}")
    private int port;

    @Value("${RABBITMQ_USERNAME:guest}")
    private String username;

    @Value("${RABBITMQ_PASSWORD:guest}")
    private String password;

    // Connection settings
    private Duration connectionTimeout = Duration.ofSeconds(30);
    private Duration requestedHeartbeat = Duration.ofSeconds(60);
    private int channelCacheSize = 25;
    private boolean publisherReturns = true;

    // Retry settings
    private int maxRetryAttempts = 3;
    private Duration initialInterval = Duration.ofSeconds(1);
    private double multiplier = 2.0;
    private Duration maxInterval = Duration.ofSeconds(10);

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        // Connection resilience settings
        factory.setConnectionTimeout((int) connectionTimeout.toMillis());
        factory.setRequestedHeartBeat((int) requestedHeartbeat.getSeconds());
        factory.setChannelCacheSize(channelCacheSize);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(publisherReturns);

        // Connection recovery settings
        factory.getRabbitConnectionFactory().setAutomaticRecoveryEnabled(true);
        factory.getRabbitConnectionFactory().setNetworkRecoveryInterval(5000);
        factory.getRabbitConnectionFactory().setTopologyRecoveryEnabled(true);

        // Enhanced connection monitoring with listeners
        factory.addConnectionListener(new org.springframework.amqp.rabbit.connection.ConnectionListener() {
            @Override
            public void onCreate(org.springframework.amqp.rabbit.connection.Connection connection) {
                logger.info("🔗 Query Service RabbitMQ connection established: localPort={}",
                        connection.getLocalPort());
            }

            @Override
            public void onClose(org.springframework.amqp.rabbit.connection.Connection connection) {
                logger.warn("🔌 Query Service RabbitMQ connection closed: localPort={}",
                        connection.getLocalPort());
            }

            @Override
            public void onShutDown(com.rabbitmq.client.ShutdownSignalException signal) {
                if (signal.isInitiatedByApplication()) {
                    logger.info("🛑 Query Service RabbitMQ connection shutdown initiated by application: reason={}",
                            signal.getReason());
                } else {
                    logger.error("💥 Query Service RabbitMQ connection shutdown unexpectedly: reason={}, cause={}",
                            signal.getReason(), signal.getCause() != null ? signal.getCause().getMessage() : "unknown");
                }
            }
        });

        logger.info(
                "✅ Query Service RabbitMQ ConnectionFactory configured for {}:{} with enhanced connection monitoring",
                host, port);

        return factory;
    }

    // Exchanges - matching Order Service
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder.directExchange(PAYMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange inventoryExchange() {
        return ExchangeBuilder.directExchange(INVENTORY_EXCHANGE)
                .durable(true)
                .build();
    }

    // Queues - matching Order Service
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "failed")
                .build();
    }

    @Bean
    public Queue orderUpdatedQueue() {
        return QueueBuilder.durable(ORDER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "failed")
                .build();
    }

    @Bean
    public Queue paymentProcessingQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "failed")
                .build();
    }

    @Bean
    public Queue inventoryReservationQueue() {
        return QueueBuilder.durable(INVENTORY_RESERVATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "failed")
                .build();
    }

    // Bindings - matching Order Service
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

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("failed");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setRetryTemplate(retryTemplate());
        template.setMandatory(true);

        // Enhanced publisher confirms with detailed correlation data logging
        template.setConfirmCallback((correlationData, ack, cause) -> {
            String correlationId = correlationData != null ? correlationData.getId() : "unknown";
            if (ack) {
                logger.debug("✅ Query service message confirmed by broker: correlationId={}, correlationData={}",
                        correlationId, correlationData);
            } else {
                logger.error("❌ Query service message NACK from broker: correlationId={}, cause={}, correlationData={}",
                        correlationId, cause, correlationData);
            }
        });

        // Enhanced returns callback with detailed message information
        template.setReturnsCallback(returned -> {
            String messageBody = returned.getMessage() != null && returned.getMessage().getBody() != null
                    ? new String(returned.getMessage().getBody())
                    : "empty";
            String correlationId = returned.getMessage() != null && returned.getMessage().getMessageProperties() != null
                    ? returned.getMessage().getMessageProperties().getCorrelationId()
                    : "unknown";

            logger.error("📤 Query service message returned undelivered: " +
                    "exchange={}, routingKey={}, replyCode={}, replyText={}, " +
                    "correlationId={}, messageBody={}, messageProperties={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    correlationId,
                    messageBody.length() > 200 ? messageBody.substring(0, 200) + "..." : messageBody,
                    returned.getMessage().getMessageProperties());
        });

        logger.info("✅ Query Service RabbitTemplate configured with enhanced publisher callbacks");
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setRetryTemplate(retryTemplate());

        // Listener container settings optimized for query processing
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(20);
        factory.setDefaultRequeueRejected(false);

        // Enhanced error handler with detailed context logging
        factory.setErrorHandler(throwable -> {
            String errorType = throwable.getClass().getSimpleName();
            String rootCause = throwable.getCause() != null ? throwable.getCause().getMessage() : "none";

            logger.error("❌ Query Service RabbitMQ listener container error: " +
                    "errorType={}, message={}, rootCause={}, stackTrace={}",
                    errorType,
                    throwable.getMessage(),
                    rootCause,
                    throwable.getStackTrace().length > 0 ? throwable.getStackTrace()[0].toString() : "unavailable");

            // Log additional context if available
            if (throwable instanceof org.springframework.amqp.AmqpException) {
                logger.error("🐰 AMQP specific error details: {}", throwable.toString());
            }
        });

        // Add consumer tag strategy for better identification
        factory.setConsumerTagStrategy(queue -> "query-service-" + queue + "-" + System.currentTimeMillis());

        logger.info("✅ Query Service RabbitMQ listener container factory configured: " +
                "concurrentConsumers={}, maxConcurrentConsumers={}, prefetchCount={}",
                2, 8, 20);

        return factory;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetryAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval.toMillis());
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval.toMillis());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    @Bean
    public MessageRecoverer messageRecoverer() {
        logger.info("🔄 Query Service RabbitMQ message recoverer configured: RejectAndDontRequeueRecoverer");
        return new RejectAndDontRequeueRecoverer();
    }

    // Log comprehensive configuration summary on startup
    @Bean
    public String rabbitMQConfigurationSummary() {
        logger.info("🚀 ========== Query Service RabbitMQ Configuration Summary ==========");
        logger.info("📡 Connection Settings:");
        logger.info("  🏠 Host: {}:{}", host, port);
        logger.info("  👤 Username: {}", username);
        logger.info("  🔐 Password: {}",
                password != null && !password.isEmpty() ? "***configured***" : "***not set***");
        logger.info("  ⏱️ Connection Timeout: {}ms", connectionTimeout.toMillis());
        logger.info("  💓 Heartbeat: {}s", requestedHeartbeat.getSeconds());
        logger.info("  📺 Channel Cache Size: {}", channelCacheSize);
        logger.info("  ✅ Publisher Confirms: CORRELATED");
        logger.info("  📤 Publisher Returns: {}", publisherReturns);
        logger.info("  🔄 Auto Recovery: enabled");
        logger.info("  🌐 Network Recovery Interval: 5000ms");
        logger.info("  🏗️ Topology Recovery: enabled");

        logger.info("🔄 Retry Configuration:");
        logger.info("  🎯 Max Retry Attempts: {}", maxRetryAttempts);
        logger.info("  ⏳ Initial Retry Interval: {}ms", initialInterval.toMillis());
        logger.info("  📈 Retry Multiplier: {}", multiplier);
        logger.info("  ⏰ Max Retry Interval: {}ms", maxInterval.toMillis());

        logger.info("👥 Consumer Configuration:");
        logger.info("  🔢 Concurrent Consumers: 2");
        logger.info("  📊 Max Concurrent Consumers: 8");
        logger.info("  📦 Prefetch Count: 20");
        logger.info("  🚫 Default Requeue Rejected: false");

        logger.info("📥 Queues & Routing:");
        logger.info("  📨 Order Created: {} -> {}", ORDER_CREATED_QUEUE, ORDER_CREATED_ROUTING_KEY);
        logger.info("  📝 Order Updated: {} -> {}", ORDER_UPDATED_QUEUE, ORDER_UPDATED_ROUTING_KEY);
        logger.info("  💳 Payment Processing: {} -> {}", PAYMENT_PROCESSING_QUEUE, PAYMENT_PROCESSING_ROUTING_KEY);
        logger.info("  📦 Inventory Reservation: {} -> {}", INVENTORY_RESERVATION_QUEUE,
                INVENTORY_RESERVATION_ROUTING_KEY);

        logger.info("🔀 Exchanges:");
        logger.info("  📋 Order Exchange: {}", ORDER_EXCHANGE);
        logger.info("  💰 Payment Exchange: {}", PAYMENT_EXCHANGE);
        logger.info("  📦 Inventory Exchange: {}", INVENTORY_EXCHANGE);

        logger.info("💀 Dead Letter Configuration:");
        logger.info("  ☠️ Dead Letter Exchange: {}", DLX_EXCHANGE);
        logger.info("  🪦 Dead Letter Queue: {}", DLQ_QUEUE);
        logger.info("  🔑 Dead Letter Routing Key: failed");

        logger.info("🎉 Query Service RabbitMQ configuration loaded successfully!");
        logger.info("===============================================================");

        return "RabbitMQ Configuration Loaded with Enhanced Logging";
    }
}