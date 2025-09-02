package com.ordersystem.unified.order.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Order item entity representing individual items within an order.
 */
@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product ID cannot be blank")
    @Column(name = "product_id", nullable = false)
    private String productId;

    @NotBlank(message = "Product name cannot be blank")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price cannot be null")
    @Positive(message = "Unit price must be positive")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Default constructor for JPA
    protected OrderItemEntity() {}

    public OrderItemEntity(String productId, String productName, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    // Business methods
    public BigDecimal getTotalPrice() {
        if (quantity != null && price != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItemEntity that = (OrderItemEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("OrderItemEntity{id=%d, productId='%s', productName='%s', quantity=%d, price=%s}",
                id, productId, productName, quantity, price);
    }
}