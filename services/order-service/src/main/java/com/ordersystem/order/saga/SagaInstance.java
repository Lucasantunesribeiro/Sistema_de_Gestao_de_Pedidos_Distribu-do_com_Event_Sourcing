package com.ordersystem.order.saga;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA Entity for persistent saga state management.
 * 
 * Provides robust persistence for distributed saga orchestration with:
 * - Automatic audit fields (created_at, updated_at)
 * - Validation constraints for data integrity
 * - JSONB storage for flexible saga data
 * - Recovery and timeout handling
 * - Type-safe status and step management
 */
@Entity
@Table(name = "saga_instances")
public class SagaInstance {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaInstance.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Id
    @Column(name = "saga_id", nullable = false, length = 255)
    @NotBlank(message = "Saga ID cannot be blank")
    private String sagaId;
    
    @Column(name = "order_id", nullable = false, length = 255)
    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 50)
    @NotNull(message = "Current step cannot be null")
    private SagaStep currentStep;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false, length = 20)
    @NotNull(message = "Saga status cannot be null")
    private SagaStatus sagaStatus;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull(message = "Created at cannot be null")
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at cannot be null")
    private Instant updatedAt;
    
    @Column(name = "timeout_at")
    private Instant timeoutAt;
    
    @Column(name = "retry_count", nullable = false)
    @Min(value = 0, message = "Retry count cannot be negative")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries", nullable = false)
    @Min(value = 0, message = "Max retries cannot be negative")
    private Integer maxRetries = 3;
    
    @Column(name = "correlation_id", length = 255)
    private String correlationId;
    
    @Column(name = "customer_id", length = 255)
    private String customerId;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount cannot be negative")
    private BigDecimal totalAmount;
    
    @Type(type = "jsonb")
    @Column(name = "saga_data", columnDefinition = "jsonb")
    private String sagaDataJson;
    
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
    
    @Type(type = "jsonb")
    @Column(name = "compensation_data", columnDefinition = "jsonb")
    private String compensationDataJson;
    
    // Transient field for saga data manipulation
    @Transient
    @JsonIgnore
    private Map<String, Object> sagaData = new HashMap<>();
    
    // Transient field for compensation data manipulation
    @Transient
    @JsonIgnore
    private Map<String, Object> compensationData = new HashMap<>();
    
    // Default constructor for JPA
    protected SagaInstance() {}
    
    /**
     * Constructor for creating new saga instance
     */
    public SagaInstance(String orderId, String customerId, BigDecimal totalAmount, String correlationId) {
        this.sagaId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.correlationId = correlationId;
        this.currentStep = SagaStep.INVENTORY_RESERVATION;
        this.sagaStatus = SagaStatus.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.timeoutAt = Instant.now().plus(5, ChronoUnit.MINUTES); // Default 5-minute timeout
        this.retryCount = 0;
        this.maxRetries = 3;
    }
    
    /**
     * Builder pattern for flexible saga creation
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Lifecycle methods for JSON serialization/deserialization
     */
    @PrePersist
    @PreUpdate
    private void serializeJsonFields() {
        this.updatedAt = Instant.now();
        
        try {
            if (sagaData != null && !sagaData.isEmpty()) {
                this.sagaDataJson = objectMapper.writeValueAsString(sagaData);
            }
            if (compensationData != null && !compensationData.isEmpty()) {
                this.compensationDataJson = objectMapper.writeValueAsString(compensationData);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing saga data for saga {}: {}", sagaId, e.getMessage());
            throw new IllegalStateException("Failed to serialize saga data", e);
        }
    }
    
    @PostLoad
    private void deserializeJsonFields() {
        try {
            if (sagaDataJson != null && !sagaDataJson.trim().isEmpty()) {
                this.sagaData = objectMapper.readValue(sagaDataJson, new TypeReference<Map<String, Object>>() {});
            } else {
                this.sagaData = new HashMap<>();
            }
            
            if (compensationDataJson != null && !compensationDataJson.trim().isEmpty()) {
                this.compensationData = objectMapper.readValue(compensationDataJson, new TypeReference<Map<String, Object>>() {});
            } else {
                this.compensationData = new HashMap<>();
            }
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing saga data for saga {}: {}", sagaId, e.getMessage());
            this.sagaData = new HashMap<>();
            this.compensationData = new HashMap<>();
        }
    }
    
    /**
     * Business logic methods
     */
    
    /**
     * Advance saga to next step
     */
    public void advanceToNextStep() {
        if (currentStep.isTerminal()) {
            throw new IllegalStateException("Cannot advance from terminal step: " + currentStep);
        }
        
        this.currentStep = currentStep.getNextStep();
        this.sagaStatus = currentStep.isTerminal() ? SagaStatus.COMPLETED : SagaStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Start compensation flow
     */
    public void startCompensation(String errorMessage) {
        this.currentStep = SagaStep.COMPENSATING;
        this.sagaStatus = SagaStatus.COMPENSATING;
        this.lastErrorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Complete saga successfully
     */
    public void complete() {
        this.currentStep = SagaStep.COMPLETED;
        this.sagaStatus = SagaStatus.COMPLETED;
        this.updatedAt = Instant.now();
        this.timeoutAt = null; // Clear timeout
    }
    
    /**
     * Fail saga permanently
     */
    public void fail(String errorMessage) {
        this.currentStep = SagaStep.FAILED;
        this.sagaStatus = SagaStatus.FAILED;
        this.lastErrorMessage = errorMessage;
        this.updatedAt = Instant.now();
        this.timeoutAt = null; // Clear timeout
    }
    
    /**
     * Increment retry count and check if max retries reached
     */
    public boolean incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
        return this.retryCount < this.maxRetries;
    }
    
    /**
     * Check if saga has timed out
     */
    public boolean hasTimedOut() {
        return timeoutAt != null && Instant.now().isAfter(timeoutAt);
    }
    
    /**
     * Check if saga can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries && sagaStatus.canBeRecovered();
    }
    
    /**
     * Reset timeout for retry
     */
    public void resetTimeout() {
        this.timeoutAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        this.updatedAt = Instant.now();
    }
    
    /**
     * Saga data manipulation methods
     */
    public void putSagaData(String key, Object value) {
        if (sagaData == null) {
            sagaData = new HashMap<>();
        }
        sagaData.put(key, value);
    }
    
    public Object getSagaData(String key) {
        return sagaData != null ? sagaData.get(key) : null;
    }
    
    public void putCompensationData(String key, Object value) {
        if (compensationData == null) {
            compensationData = new HashMap<>();
        }
        compensationData.put(key, value);
    }
    
    public Object getCompensationData(String key) {
        return compensationData != null ? compensationData.get(key) : null;
    }
    
    // Getters and Setters
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public SagaStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(SagaStep currentStep) { this.currentStep = currentStep; }
    
    public SagaStatus getSagaStatus() { return sagaStatus; }
    public void setSagaStatus(SagaStatus sagaStatus) { this.sagaStatus = sagaStatus; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Instant getTimeoutAt() { return timeoutAt; }
    public void setTimeoutAt(Instant timeoutAt) { this.timeoutAt = timeoutAt; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    
    public Map<String, Object> getSagaData() { return sagaData; }
    public void setSagaData(Map<String, Object> sagaData) { this.sagaData = sagaData; }
    
    public Map<String, Object> getCompensationData() { return compensationData; }
    public void setCompensationData(Map<String, Object> compensationData) { this.compensationData = compensationData; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SagaInstance that = (SagaInstance) o;
        return Objects.equals(sagaId, that.sagaId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sagaId);
    }
    
    @Override
    public String toString() {
        return String.format("SagaInstance{sagaId='%s', orderId='%s', currentStep=%s, sagaStatus=%s, retryCount=%d}",
                sagaId, orderId, currentStep, sagaStatus, retryCount);
    }
    
    /**
     * Builder pattern implementation
     */
    public static class Builder {
        private String orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private String correlationId;
        private Integer maxRetries = 3;
        private Instant timeoutAt;
        
        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder timeoutAt(Instant timeoutAt) {
            this.timeoutAt = timeoutAt;
            return this;
        }
        
        public SagaInstance build() {
            SagaInstance saga = new SagaInstance(orderId, customerId, totalAmount, correlationId);
            if (maxRetries != null) {
                saga.setMaxRetries(maxRetries);
            }
            if (timeoutAt != null) {
                saga.setTimeoutAt(timeoutAt);
            }
            return saga;
        }
    }
}