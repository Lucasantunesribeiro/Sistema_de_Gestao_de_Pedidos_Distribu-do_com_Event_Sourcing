package com.ordersystem.unified.inventory.model;

import com.ordersystem.unified.inventory.dto.ReservationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation entity for tracking inventory reservations
 */
@Entity
@Table(name = "reservations")
public class Reservation {
    
    @Id
    private String id;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.RESERVED;
    
    @Column(name = "warehouse_id")
    private String warehouseId;
    
    @Column(name = "correlation_id")
    private String correlationId;
    
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
    
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationItem> items;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "released_at")
    private LocalDateTime releasedAt;
    
    // Constructors
    public Reservation() {}
    
    public Reservation(String id, String orderId, LocalDateTime expiryTime) {
        this.id = id;
        this.orderId = orderId;
        this.expiryTime = expiryTime;
    }
    
    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
    
    public boolean isActive() {
        return status.isActive() && !isExpired();
    }
    
    public boolean canBeReleased() {
        return status.canBeReleased();
    }
    
    public boolean canBeConfirmed() {
        return status.canBeConfirmed() && !isExpired();
    }
    
    public void markAsExpired() {
        this.status = ReservationStatus.EXPIRED;
    }
    
    public void markAsReleased() {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
    }
    
    public void markAsConfirmed() {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
    
    public void markAsCancelled() {
        this.status = ReservationStatus.CANCELLED;
        this.releasedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public ReservationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public List<ReservationItem> getItems() {
        return items;
    }
    
    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
    
    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
    
    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }
    
    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }
    
    @Override
    public String toString() {
        return "Reservation{" +
                "id='" + id + '\'' +
                ", orderId='" + orderId + '\'' +
                ", status=" + status +
                ", warehouseId='" + warehouseId + '\'' +
                ", expiryTime=" + expiryTime +
                ", createdAt=" + createdAt +
                ", isExpired=" + isExpired() +
                '}';
    }
}