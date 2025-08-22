package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class InventoryUpdatedEvent extends BaseEvent {
    
    @JsonProperty("productId")
    private final String productId;
    
    @JsonProperty("productName")
    private final String productName;
    
    @JsonProperty("previousQuantity")
    private final Integer previousQuantity;
    
    @JsonProperty("newQuantity")
    private final Integer newQuantity;
    
    @JsonProperty("changeReason")
    private final String changeReason;
    
    @JsonProperty("updatedAt")
    private final LocalDateTime updatedAt;

    @JsonCreator
    public InventoryUpdatedEvent(@JsonProperty("productId") String productId,
                                @JsonProperty("productName") String productName,
                                @JsonProperty("previousQuantity") Integer previousQuantity,
                                @JsonProperty("newQuantity") Integer newQuantity,
                                @JsonProperty("changeReason") String changeReason,
                                @JsonProperty("correlationId") String correlationId,
                                @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "INVENTORY_UPDATED");
        this.productId = productId;
        this.productName = productName;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.changeReason = changeReason;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public String getChangeReason() { return changeReason; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}