package com.ordersystem.unified.query.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model de pedidos mantido pelo consumidor RabbitMQ a partir dos eventos do stream Order.
 * Todos os campos vêm dos eventos (inclusive datas), nunca do relógio do consumidor, para que a
 * reconstrução por replay seja determinística.
 */
@Entity
@Table(name = "order_projection")
public class OrderProjection {

    @Id
    @Column(name = "order_id", length = 255)
    private String orderId;

    @Column(name = "customer_id", length = 255)
    private String customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "last_event_version", nullable = false)
    private long lastEventVersion;

    @Column(name = "last_event_id", length = 255)
    private String lastEventId;

    @Column(name = "applied_events", nullable = false)
    private int appliedEvents;

    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    protected OrderProjection() {
    }

    public OrderProjection(String orderId, String customerId, String customerName, BigDecimal totalAmount,
                           String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    void advance(long version, String eventId, LocalDateTime occurredAt) {
        this.lastEventVersion = version;
        this.lastEventId = eventId;
        this.lastEventAt = occurredAt;
        this.appliedEvents++;
    }

    void setStatus(String status) { this.status = status; }
    void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public long getLastEventVersion() { return lastEventVersion; }
    public String getLastEventId() { return lastEventId; }
    public int getAppliedEvents() { return appliedEvents; }
    public LocalDateTime getLastEventAt() { return lastEventAt; }

    @Override
    public String toString() {
        return String.format("OrderProjection{orderId='%s', status=%s, version=%d, applied=%d}",
            orderId, status, lastEventVersion, appliedEvents);
    }
}
