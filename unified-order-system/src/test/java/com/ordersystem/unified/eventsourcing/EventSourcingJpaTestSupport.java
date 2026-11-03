package com.ordersystem.unified.eventsourcing;

import com.ordersystem.unified.config.JpaRepositoryTestApplication;
import com.ordersystem.unified.infrastructure.events.EventStore;
import com.ordersystem.unified.infrastructure.messaging.IdempotentMessageProcessor;
import com.ordersystem.unified.order.application.OrderEventSourcingRepository;
import com.ordersystem.unified.query.projection.OrderProjectionMessageHandler;
import com.ordersystem.unified.query.projection.OrderProjectionRebuilder;
import com.ordersystem.unified.query.projection.OrderProjector;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base para testes de event store / consumidor contra PostgreSQL real (Testcontainers ou embedded),
 * com o schema criado pelas migrations Flyway. Os testes NÃO rodam dentro de uma transação de teste:
 * cada operação confirma de verdade, para observar commit/rollback como em produção.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = JpaRepositoryTestApplication.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
    EventStore.class,
    OrderEventSourcingRepository.class,
    IdempotentMessageProcessor.class,
    OrderProjector.class,
    OrderProjectionMessageHandler.class,
    OrderProjectionRebuilder.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class EventSourcingJpaTestSupport extends PostgresIntegrationTestSupport {
}
