package com.ordersystem.unified.order.application;

import com.ordersystem.unified.order.domain.OrderAggregate;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconstrói o pedido a partir do event store e compara com a linha atual de {@code system_orders}.
 * Útil para auditar divergência entre estado gravado e histórico de eventos.
 */
@Service
public class OrderReplayService {

    private final OrderEventSourcingRepository orderEvents;
    private final OrderRepository orderRepository;

    public OrderReplayService(OrderEventSourcingRepository orderEvents, OrderRepository orderRepository) {
        this.orderEvents = orderEvents;
        this.orderRepository = orderRepository;
    }

    public record OrderReplayResponse(String orderId,
                                      long version,
                                      String status,
                                      BigDecimal totalAmount,
                                      String cancellationReason,
                                      String storedStatus,
                                      boolean matchesStoredState) {
    }

    @Transactional(readOnly = true)
    public OrderReplayResponse replay(String orderId) {
        OrderAggregate aggregate = orderEvents.load(orderId);
        Order stored = orderRepository.findById(orderId).orElse(null);
        if (!aggregate.exists() && stored == null) {
            throw new OrderNotFoundException("Order not found with id: " + orderId);
        }
        String replayedStatus = aggregate.exists() ? aggregate.getStatus().name() : null;
        String storedStatus = stored == null || stored.getStatus() == null ? null : stored.getStatus().name();
        return new OrderReplayResponse(
            orderId,
            aggregate.getVersion(),
            replayedStatus,
            aggregate.getTotalAmount(),
            aggregate.getCancellationReason(),
            storedStatus,
            replayedStatus != null && replayedStatus.equals(storedStatus));
    }
}
