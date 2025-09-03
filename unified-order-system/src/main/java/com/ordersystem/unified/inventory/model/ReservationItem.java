package com.ordersystem.unified.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Reservation item entity for tracking individual item reservations
 */
@Entity
@Table(name = "reservation_items")
public class ReservationItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
    
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;
    
    @Column(name = "confirmed_quantity")
    private Integer confirmedQuantity = 0;
    
    @Column(name = "released_quantity")
    private Integer releasedQuantity = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public ReservationItem() {}
    
    public ReservationItem(Reservation reservation, Product product, Stock stock, Integer requestedQuantity, Integer reservedQuantity) {
        this.reservation = reservation;
        this.product = product;
        this.stock = stock;
        this.requestedQuantity = requestedQuantity;
        this.reservedQuantity = reservedQuantity;
    }
    
    // Business methods
    public boolean isFullyReserved() {
        return reservedQuantity.equals(requestedQuantity);
    }
    
    public boolean isPartiallyReserved() {
        return reservedQuantity > 0 && reservedQuantity < requestedQuantity;
    }
    
    public Integer getUnreservedQuantity() {
        return requestedQuantity - reservedQuantity;
    }
    
    public Integer getAvailableForConfirmation() {
        return reservedQuantity - confirmedQuantity;
    }
    
    public Integer getAvailableForRelease() {
        return reservedQuantity - releasedQuantity;
    }
    
    public void confirmQuantity(Integer quantity) {
        if (quantity > getAvailableForConfirmation()) {
            throw new IllegalArgumentException("Cannot confirm more than available for confirmation");
        }
        this.confirmedQuantity += quantity;
    }
    
    public void releaseQuantity(Integer quantity) {
        if (quantity > getAvailableForRelease()) {
            throw new IllegalArgumentException("Cannot release more than available for release");
        }
        this.releasedQuantity += quantity;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Reservation getReservation() {
        return reservation;
    }
    
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public Stock getStock() {
        return stock;
    }
    
    public void setStock(Stock stock) {
        this.stock = stock;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public Integer getConfirmedQuantity() {
        return confirmedQuantity;
    }
    
    public void setConfirmedQuantity(Integer confirmedQuantity) {
        this.confirmedQuantity = confirmedQuantity;
    }
    
    public Integer getReleasedQuantity() {
        return releasedQuantity;
    }
    
    public void setReleasedQuantity(Integer releasedQuantity) {
        this.releasedQuantity = releasedQuantity;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "ReservationItem{" +
                "id='" + id + '\'' +
                ", productId='" + (product != null ? product.getId() : null) + '\'' +
                ", requestedQuantity=" + requestedQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", confirmedQuantity=" + confirmedQuantity +
                ", releasedQuantity=" + releasedQuantity +
                ", createdAt=" + createdAt +
                '}';
    }
}