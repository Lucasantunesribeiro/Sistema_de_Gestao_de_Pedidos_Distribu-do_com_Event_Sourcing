package com.ordersystem.order.model;

import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderAggregate {
    private String orderId;
    private String customerId;
    private List<OrderCreatedEvent.OrderItem> items;
    private double totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private Long version;

    public OrderAggregate() {
        this.items = new ArrayList<>();
        this.version = 0L;
    }

    public void apply(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.items = new ArrayList<>(event.getItems());
        this.totalAmount = event.getTotalAmount();
        this.status = "PENDING";
        this.createdAt = event.getTimestamp();
        this.version++;
    }

    public void apply(OrderStatusUpdatedEvent event) {
        this.status = event.getNewStatus();
        this.version++;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderCreatedEvent.OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setItems(List<OrderCreatedEvent.OrderItem> items) { this.items = items; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setVersion(Long version) { this.version = version; }
}