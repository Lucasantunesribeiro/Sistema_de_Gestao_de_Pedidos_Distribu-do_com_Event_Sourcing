package com.ordersystem.order.saga;

import com.ordersystem.shared.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Orchestration-based Saga for Order Processing
 * Coordinates: Order Creation → Inventory Reservation → Payment Processing → Confirmation/Rollback
 */
@Component
public class OrderSaga {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderSaga.class);
    private static final String INVENTORY_EXCHANGE = "inventory.fanout";
    private static final String PAYMENT_EXCHANGE = "payment.fanout";
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // In-memory saga state (for demo - use database in production)
    private final ConcurrentMap<String, SagaState> sagaStates = new ConcurrentHashMap<>();
    
    /**
     * Step 1: Order Created - Start Saga
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        logger.info("🚀 Starting Saga for Order: {}", event.getOrderId());
        
        SagaState sagaState = new SagaState(event.getOrderId());
        sagaState.setStep(SagaStep.INVENTORY_RESERVATION);
        sagaState.setStartTime(Instant.now());
        sagaStates.put(event.getOrderId(), sagaState);
        
        // Step 1: Reserve Inventory
        List<OrderItem> orderItems = event.getItems();
            
        InventoryReservationCommand command = new InventoryReservationCommand(
            event.getOrderId(),
            event.getCustomerId(),
            orderItems
        );
        
        rabbitTemplate.convertAndSend(INVENTORY_EXCHANGE, "", command);
        logger.info("📦 Inventory reservation requested for Order: {}", event.getOrderId());
    }
    
    /**
     * Step 2: Inventory Reserved - Proceed to Payment
     */
    @EventListener
    public void handleInventoryReserved(InventoryReservedEvent event) {
        logger.info("✅ Inventory reserved for Order: {}", event.getOrderId());
        
        SagaState sagaState = sagaStates.get(event.getOrderId());
        if (sagaState == null) {
            logger.error("❌ Saga state not found for Order: {}", event.getOrderId());
            return;
        }
        
        sagaState.setStep(SagaStep.PAYMENT_PROCESSING);
        
        // Step 2: Process Payment
        PaymentProcessingCommand command = new PaymentProcessingCommand(
            event.getOrderId(),
            event.getCustomerId(),
            event.getTotalAmount()
        );
        
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, "", command);
        logger.info("💳 Payment processing requested for Order: {}", event.getOrderId());
    }
    
    /**
     * Step 3: Payment Processed - Complete Saga
     */
    @EventListener
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        logger.info("💰 Payment processed for Order: {} - Status: {}", 
                   event.getOrderId(), event.getPaymentStatus());
        
        SagaState sagaState = sagaStates.get(event.getOrderId());
        if (sagaState == null) {
            logger.error("❌ Saga state not found for Order: {}", event.getOrderId());
            return;
        }
        
        if ("APPROVED".equals(event.getPaymentStatus())) {
            // Success Path: Confirm Inventory and Complete Order
            sagaState.setStep(SagaStep.COMPLETED);
            confirmInventoryAndCompleteOrder(event.getOrderId());
            logger.info("🎉 Saga completed successfully for Order: {}", event.getOrderId());
        } else {
            // Failure Path: Start Compensation
            sagaState.setStep(SagaStep.COMPENSATING);
            startCompensation(event.getOrderId(), "Payment failed: " + event.getPaymentStatus());
        }
    }
    
    /**
     * Compensation: Inventory Reservation Failed
     */
    @EventListener
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        logger.error("❌ Inventory reservation failed for Order: {} - Reason: {}", 
                    event.getOrderId(), event.getReason());
        
        SagaState sagaState = sagaStates.get(event.getOrderId());
        if (sagaState != null) {
            sagaState.setStep(SagaStep.FAILED);
            failOrder(event.getOrderId(), "Inventory reservation failed: " + event.getReason());
        }
    }
    
    /**
     * Compensation Flow
     */
    private void startCompensation(String orderId, String reason) {
        logger.warn("🔄 Starting compensation for Order: {} - Reason: {}", orderId, reason);
        
        // Release inventory reservation
        InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand(orderId, null);
        rabbitTemplate.convertAndSend(INVENTORY_EXCHANGE, "", releaseCommand);
        
        // Fail the order
        failOrder(orderId, reason);
        
        SagaState sagaState = sagaStates.get(orderId);
        if (sagaState != null) {
            sagaState.setStep(SagaStep.COMPENSATED);
        }
        
        logger.info("🔄 Compensation completed for Order: {}", orderId);
    }
    
    private void confirmInventoryAndCompleteOrder(String orderId) {
        // Confirm inventory reservation
        InventoryConfirmationCommand confirmCommand = new InventoryConfirmationCommand(orderId, null);
        rabbitTemplate.convertAndSend(INVENTORY_EXCHANGE, "", confirmCommand);
        
        // Update order status to completed
        OrderStatusUpdatedEvent statusEvent = new OrderStatusUpdatedEvent(orderId, "PENDING", "COMPLETED", LocalDateTime.now());
        rabbitTemplate.convertAndSend("order.fanout", "", statusEvent);
    }
    
    private void failOrder(String orderId, String reason) {
        OrderStatusUpdatedEvent statusEvent = new OrderStatusUpdatedEvent(orderId, "PENDING", "FAILED", LocalDateTime.now());
        rabbitTemplate.convertAndSend("order.fanout", "", statusEvent);
    }
    
    /**
     * Saga timeout handler - run periodically
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000) // Every 30 seconds
    public void handleSagaTimeouts() {
        Instant timeout = Instant.now().minus(5, ChronoUnit.MINUTES);
        
        sagaStates.entrySet().removeIf(entry -> {
            SagaState state = entry.getValue();
            if (state.getStartTime().isBefore(timeout) && 
                !state.getStep().isTerminal()) {
                
                logger.warn("⏰ Saga timeout for Order: {}", entry.getKey());
                startCompensation(entry.getKey(), "Saga timeout");
                return true;
            }
            return state.getStep().isTerminal();
        });
    }
    
    // Saga State Management
    private static class SagaState {
        private final String orderId;
        private SagaStep step;
        private Instant startTime;
        
        public SagaState(String orderId) {
            this.orderId = orderId;
        }
        
        // Getters and setters
        public String getOrderId() { return orderId; }
        public SagaStep getStep() { return step; }
        public void setStep(SagaStep step) { this.step = step; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
    }
    
    private enum SagaStep {
        INVENTORY_RESERVATION(false),
        PAYMENT_PROCESSING(false),
        COMPENSATING(false),
        COMPLETED(true),
        COMPENSATED(true),
        FAILED(true);
        
        private final boolean terminal;
        
        SagaStep(boolean terminal) {
            this.terminal = terminal;
        }
        
        public boolean isTerminal() {
            return terminal;
        }
    }
}