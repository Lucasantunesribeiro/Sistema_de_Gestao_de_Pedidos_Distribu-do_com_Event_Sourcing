package com.ordersystem.order.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Order {
    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final OrderStatus status;
    private final OffsetDateTime createdAt;

    private Order(Builder builder) {
        this.id = builder.id;
        this.customerId = builder.customerId;
        this.items = Collections.unmodifiableList(builder.items);
        this.status = builder.status;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private String customerId;
        private List<OrderItem> items = List.of();
        private OrderStatus status = OrderStatus.PENDING;
        private OffsetDateTime createdAt = OffsetDateTime.now();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder items(List<OrderItem> items) {
            this.items = List.copyOf(items);
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }
}
