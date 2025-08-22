package com.ordersystem.order.service;

import com.ordersystem.order.exception.OrderNotFoundException;
import com.ordersystem.order.exception.InvalidOrderStateException;
import com.ordersystem.order.exception.BusinessException;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.repository.OrderEventStore;
import com.ordersystem.shared.dto.CreateOrderRequest;
import com.ordersystem.shared.dto.OrderItemRequest;
import com.ordersystem.shared.dto.OrderResponse;
import com.ordersystem.shared.events.OrderItem;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.InventoryReservationCommand;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderEventStore eventStore;

    @Autowired
    private EventPublisher eventPublisher;

    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "createOrderFallback")
    @Retry(name = "database")
    public OrderResponse createOrder(@Valid CreateOrderRequest request) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Creating order for customer {}, correlationId: {}", 
                       request.getCustomerId(), correlationId);

            // Validate request
            validateCreateOrderRequest(request);

            // Convert DTOs to domain objects
            List<OrderItem> orderItems = request.getItems().stream()
                    .map(this::convertToOrderItem)
                    .collect(Collectors.toList());

            // Create order aggregate
            Order order = new Order(request.getCustomerId(), orderItems);

            // Save order events
            eventStore.saveOrder(order);

            // Publish events
            publishOrderEvents(order);

            logger.info("Order {} created successfully for customer {}", 
                       order.getOrderId(), request.getCustomerId());

            return convertToOrderResponse(order);

        } catch (Exception e) {
            logger.error("Error creating order for customer {}: {}", 
                        request.getCustomerId(), e.getMessage(), e);
            throw e;
        }
    }

    @CircuitBreaker(name = "database", fallbackMethod = "getOrderFallback")
    @Retry(name = "database")
    public OrderResponse getOrder(String orderId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.debug("Retrieving order {}, correlationId: {}", orderId, correlationId);

            Order order = eventStore.getOrder(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            return convertToOrderResponse(order);

        } catch (OrderNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "cancelOrderFallback")
    @Retry(name = "database")
    public OrderResponse cancelOrder(String orderId, String reason) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Cancelling order {}, reason: {}, correlationId: {}", 
                       orderId, reason, correlationId);

            Order order = eventStore.getOrder(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            // Business rule validation
            if (order.getStatus().getCode().equals("COMPLETED")) {
                throw new InvalidOrderStateException(orderId, order.getStatus(), 
                    "Não é possível cancelar um pedido já concluído");
            }

            order.cancel(reason);
            eventStore.saveOrder(order);
            publishOrderEvents(order);

            logger.info("Order {} cancelled successfully", orderId);

            return convertToOrderResponse(order);

        } catch (OrderNotFoundException | InvalidOrderStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void processInventoryReservation(String orderId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Processing inventory reservation for order {}, correlationId: {}", 
                       orderId, correlationId);

            Order order = eventStore.getOrder(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            // Create and publish inventory reservation command
            InventoryReservationCommand command = new InventoryReservationCommand(
                orderId, 
                order.getCustomerId(),
                order.getItems()
            );

            eventPublisher.publishInventoryReservationCommand(command);

            logger.info("Inventory reservation command sent for order {}", orderId);

        } catch (Exception e) {
            logger.error("Error processing inventory reservation for order {}: {}", 
                        orderId, e.getMessage(), e);
            throw e;
        }
    }

    // Helper methods
    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Lista de itens não pode estar vazia", "EMPTY_ITEMS");
        }

        for (OrderItemRequest item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Quantidade deve ser maior que zero", "INVALID_QUANTITY");
            }
            if (item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Preço deve ser maior que zero", "INVALID_PRICE");
            }
        }
    }

    private OrderItem convertToOrderItem(OrderItemRequest request) {
        return new OrderItem(
            request.getProductId(),
            request.getProductName(),
            request.getQuantity(),
            request.getPrice()
        );
    }

    private OrderResponse convertToOrderResponse(Order order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getCustomerId(),
            order.getStatus(),
            order.getItems(),
            order.getTotalAmount(),
            order.getPaymentStatus(),
            order.getInventoryStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private void publishOrderEvents(Order order) {
        List<Object> uncommittedEvents = order.getUncommittedEvents();
        
        for (Object event : uncommittedEvents) {
            if (event instanceof OrderCreatedEvent) {
                eventPublisher.publishOrderCreatedEvent((OrderCreatedEvent) event);
            }
            // Add other event types as needed
        }
    }

    // Fallback methods
    public OrderResponse createOrderFallback(CreateOrderRequest request, Exception ex) {
        logger.error("Circuit breaker activated for createOrder: {}", ex.getMessage());
        throw new BusinessException("Serviço temporariamente indisponível. Tente novamente em alguns instantes.", 
                                  "SERVICE_UNAVAILABLE");
    }

    public OrderResponse getOrderFallback(String orderId, Exception ex) {
        logger.error("Circuit breaker activated for getOrder: {}", ex.getMessage());
        throw new BusinessException("Serviço temporariamente indisponível. Tente novamente em alguns instantes.", 
                                  "SERVICE_UNAVAILABLE");
    }

    public OrderResponse cancelOrderFallback(String orderId, String reason, Exception ex) {
        logger.error("Circuit breaker activated for cancelOrder: {}", ex.getMessage());
        throw new BusinessException("Serviço temporariamente indisponível. Tente novamente em alguns instantes.", 
                                  "SERVICE_UNAVAILABLE");
    }
}