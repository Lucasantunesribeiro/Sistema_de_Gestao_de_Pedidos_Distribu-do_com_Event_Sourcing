package com.ordersystem.unified.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.infrastructure.events.DomainEventEntity;
import com.ordersystem.unified.infrastructure.events.DomainEventRepository;
import com.ordersystem.unified.order.application.CancelOrderUseCase;
import com.ordersystem.unified.order.application.CreateOrderUseCase;
import com.ordersystem.unified.order.application.OrderReplayService;
import com.ordersystem.unified.order.application.OrderReplayService.OrderReplayResponse;
import com.ordersystem.unified.order.dto.CancelOrderRequest;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.shared.exceptions.InsufficientInventoryException;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Fluxo real dos casos de uso (Spring + PostgreSQL) gravando o stream Order com versão esperada.
 * Sem transação de teste: commit/rollback acontecem como em produção.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(com.ordersystem.unified.config.TestConfig.class)
class OrderUseCaseEventSourcingTest extends PostgresIntegrationTestSupport {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private OrderReplayService replayService;

    @Autowired
    private DomainEventRepository domainEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void seed() {
        seedStock("ES-PROD-1", "Produto ES", new BigDecimal("40.00"), 50);
    }

    private CreateOrderRequest request(String productId, int quantity) {
        return new CreateOrderRequest("ES-CUSTOMER", "Cliente ES", "es@example.com", PaymentMethod.PIX,
            List.of(new OrderItemRequest(productId, "Produto ES", quantity, new BigDecimal("40.00"))));
    }

    @Test
    void createdOrderHasVersionedStreamAndReplayMatchesStoredState() {
        OrderResponse created = createOrderUseCase.execute(request("ES-PROD-1", 1));

        List<DomainEventEntity> stream = domainEventRepository
            .findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc("Order", created.getOrderId());
        assertThat(stream).extracting(DomainEventEntity::getAggregateVersion).containsExactly(1L, 2L, 3L, 4L);
        assertThat(stream).extracting(DomainEventEntity::getEventType).containsExactly(
            "OrderCreatedEvent", "OrderStatusUpdatedEvent", "OrderStatusUpdatedEvent", "OrderStatusUpdatedEvent");

        OrderReplayResponse replay = replayService.replay(created.getOrderId());
        assertThat(replay.status()).isEqualTo("CONFIRMED");
        assertThat(replay.version()).isEqualTo(4);
        assertThat(replay.matchesStoredState()).isTrue();
    }

    @Test
    void cancellationAppendsToStreamAndSecondForcedCancelIsRejected() {
        OrderResponse created = createOrderUseCase.execute(request("ES-PROD-1", 1));
        CancelOrderRequest cancel = new CancelOrderRequest("cliente desistiu");
        cancelOrderUseCase.execute(created.getOrderId(), cancel);

        OrderReplayResponse replay = replayService.replay(created.getOrderId());
        assertThat(replay.status()).isEqualTo("CANCELLED");
        assertThat(replay.version()).isEqualTo(5);
        assertThat(replay.matchesStoredState()).isTrue();

        CancelOrderRequest forced = new CancelOrderRequest("de novo");
        forced.setForceCancel(true);
        assertThatThrownBy(() -> cancelOrderUseCase.execute(created.getOrderId(), forced))
            .isInstanceOf(InvalidOrderException.class);
        assertThat(replayService.replay(created.getOrderId()).version()).isEqualTo(5);
    }

    @Test
    void failedCreationRollsBackOrderAndItsEventsTogether() {
        long eventsBefore = domainEventRepository.count();
        long ordersBefore = orderRepository.count();

        assertThatThrownBy(() -> createOrderUseCase.execute(request("ES-NO-STOCK", 3)))
            .isInstanceOf(InsufficientInventoryException.class);

        assertThat(domainEventRepository.count()).isEqualTo(eventsBefore);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }
}
