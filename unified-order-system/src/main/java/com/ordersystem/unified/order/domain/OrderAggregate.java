package com.ordersystem.unified.order.domain;

import com.ordersystem.unified.domain.events.BaseEvent;
import com.ordersystem.unified.domain.events.OrderCancelledEvent;
import com.ordersystem.unified.domain.events.OrderCreatedEvent;
import com.ordersystem.unified.domain.events.OrderItem;
import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.domain.events.OrderStatusUpdatedEvent;
import com.ordersystem.unified.shared.exceptions.EventStreamCorruptedException;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agregado Order reconstruído a partir do event store (stream {@code aggregate_type = 'Order'}).
 *
 * <p>Regras:
 * <ul>
 *   <li>O estado só muda via {@link #apply(BaseEvent)}; comandos validam e registram eventos novos.</li>
 *   <li>{@link #rehydrate(String, List)} exige versões contíguas a partir de 1 e o primeiro evento
 *       {@link OrderCreatedEvent}; qualquer desvio é {@link EventStreamCorruptedException}.</li>
 *   <li>{@link #getCommittedVersion()} é a versão esperada usada na concorrência otimista ao gravar.</li>
 *   <li>Nenhum valor de relógio é lido no replay: reconstruir o mesmo stream duas vezes produz o mesmo estado.</li>
 * </ul>
 */
public final class OrderAggregate {

    public static final String AGGREGATE_TYPE = "Order";

    private final String orderId;
    private String customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private List<OrderItem> items = List.of();
    private OrderStatus status;
    private String cancellationReason;

    private long committedVersion;
    private long version;
    private final List<BaseEvent> uncommittedEvents = new ArrayList<>();

    private OrderAggregate(String orderId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
    }

    /** Agregado vazio (stream inexistente): versão 0. */
    public static OrderAggregate empty(String orderId) {
        return new OrderAggregate(orderId);
    }

    /** Comando de criação: registra {@link OrderCreatedEvent} (status inicial PENDING). */
    public static OrderAggregate create(String orderId,
                                        String customerId,
                                        String customerName,
                                        List<OrderItem> items,
                                        BigDecimal totalAmount,
                                        String correlationId) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("Customer ID is required");
        }
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new InvalidOrderException("Total amount must be positive");
        }
        OrderAggregate aggregate = new OrderAggregate(orderId);
        aggregate.record(new OrderCreatedEvent(
            orderId, customerId, customerName,
            items == null ? List.of() : List.copyOf(items),
            totalAmount, correlationId, null));
        return aggregate;
    }

    /**
     * Reconstrói o agregado aplicando os eventos em ordem de versão.
     * A lista pode vir em qualquer ordem; é ordenada por versão e validada (sem lacunas/duplicatas).
     */
    public static OrderAggregate rehydrate(String orderId, List<RecordedOrderEvent> history) {
        OrderAggregate aggregate = new OrderAggregate(orderId);
        List<RecordedOrderEvent> ordered = new ArrayList<>(history);
        ordered.sort((a, b) -> Long.compare(a.version(), b.version()));
        for (RecordedOrderEvent recorded : ordered) {
            long expected = aggregate.version + 1;
            if (recorded.version() != expected) {
                throw new EventStreamCorruptedException(String.format(
                    "Order %s: expected event version %d but found %d", orderId, expected, recorded.version()));
            }
            try {
                aggregate.apply(recorded.event());
            } catch (InvalidOrderException exception) {
                throw new EventStreamCorruptedException(String.format(
                    "Order %s: event v%d cannot be applied: %s", orderId, recorded.version(), exception.getMessage()),
                    exception);
            }
            aggregate.version = recorded.version();
        }
        aggregate.committedVersion = aggregate.version;
        return aggregate;
    }

    /** Comando: muda o status validando a máquina de estados de {@link OrderStatus}. */
    public void changeStatus(OrderStatus newStatus, String reason, String correlationId) {
        requireExists();
        if (newStatus == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Use cancel(...) to cancel an order");
        }
        ensureTransition(newStatus);
        record(new OrderStatusUpdatedEvent(
            orderId, customerId, status.name(), newStatus.name(), reason, correlationId, null));
    }

    /** Valida, sem registrar evento, se o pedido pode ser cancelado (usado antes das compensações). */
    public void ensureCanBeCancelled() {
        requireExists();
        ensureTransition(OrderStatus.CANCELLED);
    }

    /** Comando: cancela o pedido. */
    public void cancel(String reason,
                       String cancelledBy,
                       String correlationId,
                       String reservationId,
                       String paymentId,
                       boolean inventoryReleased,
                       boolean paymentRefunded) {
        ensureCanBeCancelled();
        record(OrderCancelledEvent.builder()
            .orderId(orderId)
            .customerId(customerId)
            .totalAmount(totalAmount)
            .cancellationReason(reason)
            .cancelledBy(cancelledBy)
            .correlationId(correlationId)
            .reservationId(reservationId)
            .paymentId(paymentId)
            .inventoryReleased(inventoryReleased)
            .paymentRefunded(paymentRefunded)
            .build());
    }

    /** Marca os eventos pendentes como gravados (chamado pelo repositório após o append). */
    public void markCommitted() {
        uncommittedEvents.clear();
        committedVersion = version;
    }

    private void record(BaseEvent event) {
        apply(event);
        version++;
        uncommittedEvents.add(event);
    }

    private void apply(BaseEvent event) {
        if (event instanceof OrderCreatedEvent created) {
            if (status != null) {
                throw new InvalidOrderException("Order " + orderId + " already created");
            }
            if (!orderId.equals(created.getOrderId())) {
                throw new InvalidOrderException("Event belongs to order " + created.getOrderId());
            }
            customerId = created.getCustomerId();
            customerName = created.getCustomerName();
            totalAmount = created.getTotalAmount();
            items = created.getItems() == null ? List.of() : List.copyOf(created.getItems());
            status = OrderStatus.PENDING;
        } else if (event instanceof OrderStatusUpdatedEvent updated) {
            requireExists();
            OrderStatus target = parseStatus(updated.getNewStatus());
            if (updated.getOldStatus() != null && !updated.getOldStatus().equals(status.name())) {
                throw new InvalidOrderException(String.format(
                    "Event expects status %s but order is %s", updated.getOldStatus(), status));
            }
            ensureTransition(target);
            status = target;
        } else if (event instanceof OrderCancelledEvent cancelled) {
            requireExists();
            ensureTransition(OrderStatus.CANCELLED);
            status = OrderStatus.CANCELLED;
            cancellationReason = cancelled.getCancellationReason();
        } else {
            throw new EventStreamCorruptedException(
                "Unsupported event for Order aggregate: " + event.getClass().getSimpleName());
        }
    }

    private void ensureTransition(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderException(String.format(
                "Invalid status transition for order %s: %s -> %s", orderId, status, target));
        }
    }

    private void requireExists() {
        if (status == null) {
            throw new InvalidOrderException("Order " + orderId + " does not exist in the event store");
        }
    }

    private static OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidOrderException("Unknown order status in event: " + value);
        }
    }

    public boolean exists() {
        return status != null;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    /** Versão após aplicar eventos gravados e pendentes. */
    public long getVersion() { return version; }
    /** Versão já gravada no event store (versão esperada no próximo append). */
    public long getCommittedVersion() { return committedVersion; }
    public List<BaseEvent> getUncommittedEvents() { return Collections.unmodifiableList(uncommittedEvents); }
}
