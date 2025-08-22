package com.ordersystem.order.model;

import com.ordersystem.shared.events.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {
    
    private String orderId;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private InventoryStatus inventoryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    
    // Event sourcing fields
    private List<Object> uncommittedEvents;
    private List<Object> events;

    // Default constructor
    public Order() {
        this.uncommittedEvents = new ArrayList<>();
        this.events = new ArrayList<>();
        this.version = 0;
    }

    // Constructor for creating new order
    public Order(String customerId, List<OrderItem> items) {
        this();
        this.orderId = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.totalAmount = calculateTotalAmount(items);
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.PENDING;
        this.inventoryStatus = InventoryStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Apply order created event
        OrderCreatedEvent event = new OrderCreatedEvent(
            this.orderId, 
            this.customerId, 
            this.items, 
            this.totalAmount, 
            this.createdAt
        );
        
        applyEvent(event);
    }

    // Event sourcing methods
    public void applyEvent(Object event) {
        if (event instanceof OrderCreatedEvent) {
            apply((OrderCreatedEvent) event);
        } else if (event instanceof OrderStatusUpdatedEvent) {
            apply((OrderStatusUpdatedEvent) event);
        } else if (event instanceof PaymentProcessedEvent) {
            apply((PaymentProcessedEvent) event);
        } else if (event instanceof InventoryReservedEvent) {
            apply((InventoryReservedEvent) event);
        } else if (event instanceof InventoryReservationFailedEvent) {
            apply((InventoryReservationFailedEvent) event);
        } else if (event instanceof OrderCompletedEvent) {
            apply((OrderCompletedEvent) event);
        } else if (event instanceof OrderCancelledEvent) {
            apply((OrderCancelledEvent) event);
        } else if (event instanceof PaymentFailedEvent) {
            apply((PaymentFailedEvent) event);
        }
        
        this.events.add(event);
        this.uncommittedEvents.add(event);
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    // Event application methods
    private void apply(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.items = event.getItems();
        this.totalAmount = event.getTotalAmount();
        this.status = OrderStatus.PENDING;
        this.createdAt = event.getTimestamp();
        this.updatedAt = event.getTimestamp();
    }

    private void apply(OrderStatusUpdatedEvent event) {
        this.status = OrderStatus.fromCode(event.getNewStatus());
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(PaymentProcessedEvent event) {
        this.paymentStatus = PaymentStatus.fromCode(event.getPaymentStatus());
        if ("APPROVED".equals(event.getPaymentStatus())) {
            this.status = OrderStatus.PAYMENT_APPROVED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(InventoryReservedEvent event) {
        this.inventoryStatus = InventoryStatus.RESERVED;
        this.status = OrderStatus.INVENTORY_RESERVED;
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(InventoryReservationFailedEvent event) {
        this.inventoryStatus = InventoryStatus.OUT_OF_STOCK;
        this.status = OrderStatus.INVENTORY_RESERVATION_FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(OrderCompletedEvent event) {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(PaymentFailedEvent event) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.status = OrderStatus.PAYMENT_FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void reserveInventory() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot reserve inventory for order in status: " + this.status);
        }
        
        InventoryReservationCommand command = new InventoryReservationCommand(
            this.orderId, 
            this.customerId, 
            this.items
        );
        
        applyEvent(command);
    }

    public void processPayment() {
        if (this.status != OrderStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException("Cannot process payment for order in status: " + this.status);
        }
        
        PaymentProcessingCommand command = new PaymentProcessingCommand(
            this.orderId, 
            this.customerId, 
            this.totalAmount
        );
        
        applyEvent(command);
    }

    public void complete() {
        if (this.status != OrderStatus.PAYMENT_APPROVED) {
            throw new IllegalStateException("Cannot complete order in status: " + this.status);
        }
        
        OrderCompletedEvent event = new OrderCompletedEvent(
            this.orderId,
            this.customerId,
            this.items,
            this.totalAmount,
            UUID.randomUUID().toString(), // paymentId
            UUID.randomUUID().toString(), // correlationId
            null // causationId
        );
        
        applyEvent(event);
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order in status: " + this.status);
        }
        
        OrderCancelledEvent event = new OrderCancelledEvent(
            this.orderId,
            this.customerId,
            reason,
            UUID.randomUUID().toString(), // correlationId
            null // causationId
        );
        
        applyEvent(event);
    }

    // Helper methods
    private BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markEventsAsCommitted() {
        this.uncommittedEvents.clear();
    }

    public List<Object> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    // Static factory method for reconstruction from events
    public static Order fromEvents(List<Object> events) {
        Order order = new Order();
        for (Object event : events) {
            order.applyEvent(event);
        }
        order.markEventsAsCommitted();
        return order;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public InventoryStatus getInventoryStatus() { return inventoryStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getVersion() { return version; }
    public List<Object> getEvents() { return events; }
}