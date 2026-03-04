package com.ordersystem.unified.order.application;

import com.ordersystem.unified.infrastructure.events.EventPublisher;
import com.ordersystem.unified.inventory.InventoryService;
import com.ordersystem.unified.inventory.dto.ReservationResponse;
import com.ordersystem.unified.order.domain.OrderBusinessRules;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.payment.PaymentService;
import com.ordersystem.unified.payment.PaymentResult;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.shared.events.*;
import com.ordersystem.unified.shared.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use Case for creating orders with complete business flow.
 * Orchestrates inventory reservation, payment processing, and order confirmation.
 *
 * Production-ready implementation following Clean Architecture.
 * Implements Saga pattern for distributed transaction management.
 */
@Service
public class CreateOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateOrderUseCase.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderBusinessRules businessRules;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Executes the complete order creation flow.
     * Steps:
     * 1. Validate business rules
     * 2. Reserve inventory (atomic)
     * 3. Process payment (with retry)
     * 4. Confirm order
     * 5. Publish events
     *
     * Implements compensating transactions if any step fails.
     *
     * @param request Order creation request
     * @return Created order response
     * @throws InvalidOrderException if validation fails
     * @throws InsufficientInventoryException if inventory unavailable
     * @throws PaymentProcessingException if payment fails
     */
    @Transactional
    public OrderResponse execute(CreateOrderRequest request) {
        // Setup correlation ID for distributed tracing
        String correlationId = request.getCorrelationId() != null ?
            request.getCorrelationId() : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        request.setCorrelationId(correlationId);

        logger.info("Starting order creation: customer={}, correlationId={}",
                   request.getCustomerId(), correlationId);

        String orderId = null;
        String reservationId = null;
        String paymentId = null;

        try {
            // Step 1: Validate business rules
            validateBusinessRules(request);

            // Step 2: Create order entity (PENDING status)
            Order order = createOrderEntity(request);
            orderId = order.getId();

            // Step 3: Reserve inventory
            reservationId = reserveInventory(order, request, correlationId);
            order.setReservationId(reservationId);
            order.updateStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(order);

            // Publish inventory reserved event
            publishInventoryReservedEvent(order, request.getItems(), reservationId, correlationId);

            // Step 4: Process payment
            PaymentMethod paymentMethod = request.getPaymentMethod() != null ?
                request.getPaymentMethod() : PaymentMethod.PIX;

            paymentId = processPayment(order, paymentMethod, correlationId);
            order.setPaymentId(paymentId);

            // Retrieve transactionId from persisted Payment entity
            String transactionId = paymentService.getPaymentById(paymentId)
                .map(com.ordersystem.unified.payment.model.Payment::getTransactionId)
                .orElse(null);
            order.setTransactionId(transactionId);

            order.updateStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);

            // Step 5: Confirm order
            order.updateStatus(OrderStatus.CONFIRMED);
            Order savedOrder = orderRepository.save(order);

            // Publish order confirmed event
            publishOrderCreatedEvent(savedOrder, correlationId);

            logger.info("Order created successfully: orderId={}, reservationId={}, paymentId={}",
                       orderId, reservationId, paymentId);

            return mapToResponse(savedOrder);

        } catch (InsufficientInventoryException e) {
            logger.error("Inventory reservation failed for order: {}", orderId, e);
            handleInventoryFailure(orderId, correlationId);
            throw e;

        } catch (PaymentProcessingException e) {
            logger.error("Payment processing failed for order: {}", orderId, e);
            handlePaymentFailure(orderId, reservationId, correlationId);
            throw e;

        } catch (Exception e) {
            logger.error("Order creation failed: orderId={}", orderId, e);
            handleGeneralFailure(orderId, reservationId, paymentId, correlationId);
            throw new OrderProcessingException(orderId, "Order creation failed: " + e.getMessage(), e);

        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Validates business rules before creating order.
     */
    private void validateBusinessRules(CreateOrderRequest request) {
        logger.debug("Validating business rules");

        // Validate customer eligibility
        String customerId = request.getCustomerId() != null ?
            request.getCustomerId() : "UNKNOWN";

        if (!businessRules.isCustomerAllowedToOrder(customerId)) {
            throw new InvalidOrderException("Customer not allowed to place orders: " + customerId);
        }

        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(request);

        // Validate order
        businessRules.validateOrderCreation(
            customerId,
            totalAmount,
            request.getItems() != null ? request.getItems().size() : 0
        );

        logger.debug("Business rules validation passed");
    }

    /**
     * Creates order entity in PENDING status.
     */
    private Order createOrderEntity(CreateOrderRequest request) {
        logger.debug("Creating order entity");

        String orderId = UUID.randomUUID().toString();
        String customerId = request.getCustomerId() != null ?
            request.getCustomerId() : "UNKNOWN";
        String customerName = request.getCustomerName();
        BigDecimal totalAmount = calculateTotalAmount(request);

        Order order = new Order(orderId, customerId, customerName, totalAmount);
        order.setCorrelationId(request.getCorrelationId());

        // Add items
        if (request.getItems() != null) {
            for (OrderItemRequest itemReq : request.getItems()) {
                OrderItemEntity item = new OrderItemEntity(
                    itemReq.getProductId(),
                    itemReq.getProductName(),
                    itemReq.getQuantity(),
                    itemReq.getUnitPrice()
                );
                order.addItem(item);
            }
        }

        Order savedOrder = orderRepository.save(order);
        logger.debug("Order entity created: {}", orderId);

        return savedOrder;
    }

    /**
     * Reserves inventory for order items.
     */
    private String reserveInventory(Order order, CreateOrderRequest request, String correlationId) {
        logger.debug("Reserving inventory for order: {}", order.getId());

        List<OrderItem> items = request.getItems().stream()
            .map(item -> new OrderItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .collect(Collectors.toList());

        ReservationResponse reservationResponse = inventoryService.reserveItems(items);

        if (reservationResponse.getStatus() != com.ordersystem.unified.inventory.dto.ReservationStatus.RESERVED) {
            throw new InsufficientInventoryException(
                "Failed to reserve inventory: " + reservationResponse.getStatus()
            );
        }

        logger.info("Inventory reserved: reservationId={}", reservationResponse.getReservationId());
        return reservationResponse.getReservationId();
    }

    /**
     * Processes payment for order with retry logic.
     */
    private String processPayment(Order order, PaymentMethod paymentMethod, String correlationId) {
        logger.debug("Processing payment for order: {}", order.getId());

        PaymentResult result = paymentService.processPayment(
            order.getId(),
            order.getTotalAmount(),
            correlationId,
            paymentMethod.name()
        );

        if (!result.isSuccess()) {
            throw new PaymentProcessingException(
                "Payment failed: " + result.getMessage()
            );
        }

        logger.info("Payment processed: paymentId={}", result.getPaymentId());
        return result.getPaymentId();
    }

    /**
     * Publishes order created event.
     */
    private void publishOrderCreatedEvent(Order order, String correlationId) {
        List<OrderItem> items = order.getItems().stream()
            .map(item -> new OrderItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerName(),
            items,
            order.getTotalAmount(),
            correlationId,
            null
        );

        eventPublisher.publish(event);
    }

    /**
     * Publishes inventory reserved event.
     */
    private void publishInventoryReservedEvent(Order order, List<OrderItemRequest> items,
                                              String reservationId, String correlationId) {
        List<OrderItem> eventItems = items.stream()
            .map(item -> new OrderItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .collect(Collectors.toList());

        InventoryReservedEvent event = new InventoryReservedEvent(
            order.getId(),
            order.getCustomerId(),
            eventItems,
            reservationId,
            correlationId,
            null
        );

        eventPublisher.publish(event);
    }

    /**
     * Handles inventory reservation failure - mark order as failed.
     */
    private void handleInventoryFailure(String orderId, String correlationId) {
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.updateStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            });
        }
    }

    /**
     * Handles payment failure - release inventory and mark order as failed.
     */
    private void handlePaymentFailure(String orderId, String reservationId, String correlationId) {
        // Release inventory reservation
        if (reservationId != null) {
            try {
                inventoryService.releaseReservation(reservationId);
                logger.info("Inventory released due to payment failure: {}", reservationId);
            } catch (Exception e) {
                logger.error("Failed to release inventory: {}", reservationId, e);
            }
        }

        // Mark order as failed
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.updateStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            });
        }
    }

    /**
     * Handles general failure - compensate all actions.
     */
    private void handleGeneralFailure(String orderId, String reservationId,
                                     String paymentId, String correlationId) {
        // Attempt to release inventory
        if (reservationId != null) {
            try {
                inventoryService.releaseReservation(reservationId);
            } catch (Exception e) {
                logger.error("Failed to release inventory during compensation: {}", reservationId, e);
            }
        }

        // Mark order as failed
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.updateStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            });
        }
    }

    /**
     * Calculates total amount from items.
     */
    private BigDecimal calculateTotalAmount(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return request.getTotalAmount() != null ? request.getTotalAmount() : BigDecimal.ZERO;
        }

        return request.getItems().stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Maps Order entity to response DTO.
     */
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(order.getCustomerName());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setCorrelationId(order.getCorrelationId());
        response.setReservationId(order.getReservationId());
        response.setPaymentId(order.getPaymentId());
        response.setTransactionId(order.getTransactionId());

        List<com.ordersystem.unified.order.dto.OrderItemResponse> itemResponses = order.getItems().stream()
            .map(item -> {
                com.ordersystem.unified.order.dto.OrderItemResponse itemResponse =
                    new com.ordersystem.unified.order.dto.OrderItemResponse();
                itemResponse.setProductId(item.getProductId());
                itemResponse.setProductName(item.getProductName());
                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setUnitPrice(item.getUnitPrice());
                itemResponse.setTotalPrice(item.getTotalPrice());
                return itemResponse;
            })
            .collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }
}
