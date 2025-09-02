package com.ordersystem.unified.orchestration;

import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.OrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.PaymentService;
import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.inventory.InventoryService;
import com.ordersystem.unified.inventory.InventoryResult;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.OrderProcessingException;
import com.ordersystem.unified.shared.exceptions.PaymentException;
import com.ordersystem.unified.shared.exceptions.InsufficientInventoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orchestration service for managing complex order workflows.
 * Handles transactions across Order, Payment, and Inventory modules.
 */
@Service
public class OrderOrchestrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderOrchestrationService.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    /**
     * Process complete order workflow with proper transaction management.
     * This method orchestrates inventory reservation, payment processing, and order confirmation.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse processCompleteOrder(OrderRequest orderRequest) {
        String correlationId = UUID.randomUUID().toString();
        logger.info("Starting order processing workflow - correlationId: {}, customerId: {}", 
                   correlationId, orderRequest.getCustomerId());
        
        try {
            // Step 1: Create pending order
            OrderResponse order = orderService.createOrder(orderRequest);
            logger.info("Order created - orderId: {}, correlationId: {}", order.getOrderId(), correlationId);
            
            // Step 2: Reserve inventory for all items
            reserveInventoryForOrder(order, correlationId);
            
            // Step 3: Process payment
            Payment payment = processPaymentForOrder(order, correlationId);
            
            // Step 4: Confirm order if payment successful
            if (payment.isSuccessful()) {
                OrderResponse confirmedOrder = orderService.confirmOrder(order.getOrderId());
                logger.info("Order processing completed successfully - orderId: {}, correlationId: {}", 
                           order.getOrderId(), correlationId);
                return confirmedOrder;
            } else {
                // Payment failed - rollback inventory and cancel order
                rollbackOrderProcessing(order, correlationId, "Payment failed");
                throw new PaymentException("Payment processing failed for order: " + order.getOrderId());
            }
            
        } catch (Exception e) {
            logger.error("Order processing failed - correlationId: {}, error: {}", correlationId, e.getMessage());
            throw new OrderProcessingException("Failed to process order: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reserve inventory for all items in the order.
     */
    private void reserveInventoryForOrder(OrderResponse order, String correlationId) {
        logger.debug("Reserving inventory for order - orderId: {}, correlationId: {}", 
                    order.getOrderId(), correlationId);
        
        order.getItems().forEach(item -> {
            try {
                InventoryResult result = inventoryService.reserveInventory(
                    item.getProductId(), 
                    item.getQuantity()
                );
                
                if (!result.isSuccess()) {
                    throw new InsufficientInventoryException(
                        "Insufficient inventory for product: " + item.getProductId() + 
                        ". Available: " + result.getAvailableQuantity() + 
                        ", Required: " + item.getQuantity()
                    );
                }
                
                logger.debug("Inventory reserved - productId: {}, quantity: {}, correlationId: {}", 
                           item.getProductId(), item.getQuantity(), correlationId);
                           
            } catch (Exception e) {
                logger.error("Failed to reserve inventory - productId: {}, correlationId: {}, error: {}", 
                           item.getProductId(), correlationId, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Process payment for the order.
     */
    private Payment processPaymentForOrder(OrderResponse order, String correlationId) {
        logger.debug("Processing payment for order - orderId: {}, amount: {}, correlationId: {}", 
                    order.getOrderId(), order.getTotalAmount(), correlationId);
        
        try {
            Payment payment = paymentService.processPayment(
                order.getOrderId(),
                order.getTotalAmount(),
                order.getCustomerId()
            );
            
            logger.debug("Payment processed - orderId: {}, paymentId: {}, successful: {}, correlationId: {}", 
                        order.getOrderId(), payment.getPaymentId(), payment.isSuccessful(), correlationId);
            
            return payment;
            
        } catch (Exception e) {
            logger.error("Payment processing failed - orderId: {}, correlationId: {}, error: {}", 
                        order.getOrderId(), correlationId, e.getMessage());
            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Rollback order processing in case of failures.
     */
    private void rollbackOrderProcessing(OrderResponse order, String correlationId, String reason) {
        logger.warn("Rolling back order processing - orderId: {}, reason: {}, correlationId: {}", 
                   order.getOrderId(), reason, correlationId);
        
        try {
            // Release reserved inventory
            order.getItems().forEach(item -> {
                try {
                    inventoryService.releaseReservation(item.getProductId(), item.getQuantity());
                    logger.debug("Inventory reservation released - productId: {}, quantity: {}, correlationId: {}", 
                               item.getProductId(), item.getQuantity(), correlationId);
                } catch (Exception e) {
                    logger.error("Failed to release inventory reservation - productId: {}, correlationId: {}, error: {}", 
                               item.getProductId(), correlationId, e.getMessage());
                }
            });
            
            // Cancel order
            orderService.cancelOrder(order.getOrderId(), reason);
            logger.info("Order rollback completed - orderId: {}, correlationId: {}", 
                       order.getOrderId(), correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to rollback order processing - orderId: {}, correlationId: {}, error: {}", 
                        order.getOrderId(), correlationId, e.getMessage());
        }
    }
    
    /**
     * Cancel an existing order with proper cleanup.
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderWithCleanup(String orderId, String reason) {
        String correlationId = UUID.randomUUID().toString();
        logger.info("Canceling order with cleanup - orderId: {}, reason: {}, correlationId: {}", 
                   orderId, reason, correlationId);
        
        try {
            OrderResponse order = orderService.getOrder(orderId);
            
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                // Release inventory for confirmed orders
                order.getItems().forEach(item -> {
                    inventoryService.releaseReservation(item.getProductId(), item.getQuantity());
                });
            }
            
            // Cancel the order
            orderService.cancelOrder(orderId, reason);
            
            logger.info("Order cancellation completed - orderId: {}, correlationId: {}", 
                       orderId, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to cancel order - orderId: {}, correlationId: {}, error: {}", 
                        orderId, correlationId, e.getMessage());
            throw new OrderProcessingException("Failed to cancel order: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retry failed order processing with exponential backoff.
     */
    public OrderResponse retryOrderProcessing(String orderId, int maxRetries) {
        String correlationId = UUID.randomUUID().toString();
        logger.info("Retrying order processing - orderId: {}, maxRetries: {}, correlationId: {}", 
                   orderId, maxRetries, correlationId);
        
        OrderResponse order = orderService.getOrder(orderId);
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderProcessingException("Order is not in pending status: " + order.getStatus());
        }
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("Retry attempt {} for order - orderId: {}, correlationId: {}", 
                           attempt, orderId, correlationId);
                
                // Convert OrderResponse back to OrderRequest for retry
                OrderRequest retryRequest = convertToOrderRequest(order);
                return processCompleteOrder(retryRequest);
                
            } catch (Exception e) {
                logger.warn("Retry attempt {} failed for order - orderId: {}, correlationId: {}, error: {}", 
                           attempt, orderId, correlationId, e.getMessage());
                
                if (attempt == maxRetries) {
                    logger.error("All retry attempts exhausted for order - orderId: {}, correlationId: {}", 
                               orderId, correlationId);
                    throw new OrderProcessingException("Order processing failed after " + maxRetries + " attempts", e);
                }
                
                // Exponential backoff
                try {
                    Thread.sleep(1000L * attempt * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new OrderProcessingException("Retry interrupted", ie);
                }
            }
        }
        
        throw new OrderProcessingException("Unexpected end of retry loop");
    }
    
    /**
     * Convert OrderResponse back to OrderRequest for retry scenarios.
     */
    private OrderRequest convertToOrderRequest(OrderResponse order) {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(order.getCustomerId());
        request.setItems(order.getItems());
        return request;
    }
}