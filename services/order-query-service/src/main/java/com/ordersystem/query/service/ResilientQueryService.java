package com.ordersystem.query.service;

import com.ordersystem.query.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.concurrent.Executor;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Resilient Query Service with intelligent fallback strategies
 * Implements graceful degradation for 99.9% availability
 */
@Service
public class ResilientQueryService {

    private static final Logger log = LoggerFactory.getLogger(ResilientQueryService.class);

    private final ExternalPaymentService externalPaymentService;
    private final ExternalInventoryService externalInventoryService;
    private final CacheService cacheService;
    private final PaymentQueueService paymentQueueService;
    private final BackupPaymentProvider backupPaymentProvider;

    @Autowired
    public ResilientQueryService(
            ExternalPaymentService externalPaymentService,
            ExternalInventoryService externalInventoryService,
            CacheService cacheService,
            PaymentQueueService paymentQueueService,
            BackupPaymentProvider backupPaymentProvider) {
        this.externalPaymentService = externalPaymentService;
        this.externalInventoryService = externalInventoryService;
        this.cacheService = cacheService;
        this.paymentQueueService = paymentQueueService;
        this.backupPaymentProvider = backupPaymentProvider;
    }

    /**
     * Process payment with intelligent fallback strategies using isolated thread pool
     */
    @Async("payment-executor")
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    public CompletableFuture<PaymentResult> processPayment(PaymentRequest request) {
        log.debug("Processing payment for order: {} on thread: {}", request.getOrderId(), 
            Thread.currentThread().getName());
        return CompletableFuture.completedFuture(externalPaymentService.processPayment(request));
    }

    /**
     * Fallback payment processing with multiple strategies
     */
    public CompletableFuture<PaymentResult> fallbackPayment(PaymentRequest request, Exception ex) {
        log.warn("Payment service circuit open, using fallback strategy for order {}: {}", 
            request.getOrderId(), ex.getMessage());
        
        // Strategy 1: Queue for later processing on temporary failures
        if (isTemporaryFailure(ex)) {
            paymentQueueService.queuePaymentForRetry(request);
            return CompletableFuture.completedFuture(
                PaymentResult.queued("Payment queued for retry due to temporary service issues")
            );
        }
        
        // Strategy 2: Use backup payment provider for provider-specific failures
        if (isProviderSpecificFailure(ex)) {
            try {
                return backupPaymentProvider.processPayment(request);
            } catch (Exception backupEx) {
                log.error("Backup payment provider also failed for order {}: {}", 
                    request.getOrderId(), backupEx.getMessage());
            }
        }
        
        // Strategy 3: Manual approval workflow as last resort
        return CompletableFuture.completedFuture(
            PaymentResult.pendingApproval("Payment requires manual approval due to service issues")
        );
    }

    /**
     * Check stock level with cache fallback using isolated thread pool
     */
    @Async("inventory-executor")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackInventoryCheck")
    public CompletableFuture<StockLevel> checkStockLevel(String productId) {
        log.debug("Checking stock level for product: {} on thread: {}", productId, 
            Thread.currentThread().getName());
        return CompletableFuture.completedFuture(externalInventoryService.checkStockLevel(productId));
    }

    /**
     * Fallback inventory check using cached data
     */
    public CompletableFuture<StockLevel> fallbackInventoryCheck(String productId, Exception ex) {
        log.warn("Inventory service unavailable for product {}, using fallback: {}", 
            productId, ex.getMessage());
        
        // Fallback to cached data
        Optional<StockLevel> cachedLevel = cacheService.getCachedStockLevel(productId);
        if (cachedLevel.isPresent() && !cachedLevel.get().isStale()) {
            log.info("Using cached inventory data for product: {}", productId);
            return CompletableFuture.completedFuture(cachedLevel.get());
        }
        
        // Conservative fallback: assume limited stock
        log.warn("Inventory service unavailable, using conservative stock estimate for: {}", productId);
        return CompletableFuture.completedFuture(StockLevel.limited(productId, 5)); // Conservative estimate
    }

    /**
     * Reserve stock with queue fallback using isolated thread pool
     */
    @Async("inventory-executor")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackReserveStock")
    public CompletableFuture<ReservationResult> reserveStock(String productId, int quantity) {
        log.debug("Reserving stock for product: {}, quantity: {} on thread: {}", 
            productId, quantity, Thread.currentThread().getName());
        return CompletableFuture.completedFuture(externalInventoryService.reserveStock(productId, quantity));
    }

    /**
     * Fallback stock reservation with queuing
     */
    public CompletableFuture<ReservationResult> fallbackReserveStock(String productId, int quantity, Exception ex) {
        log.warn("Stock reservation failed for product {}, queuing for later processing: {}", 
            productId, ex.getMessage());
        
        // Queue reservation for later processing
        cacheService.queueStockReservation(productId, quantity);
        
        return CompletableFuture.completedFuture(ReservationResult.deferred(
            "Stock reservation queued due to service unavailability"
        ));
    }

    /**
     * Get order summary with partial data support using isolated thread pool
     */
    @Async("query-executor")
    public CompletableFuture<OrderSummary> getOrderSummary(String orderId) {
        log.debug("Getting order summary for: {}", orderId);
        
        OrderSummary summary = new OrderSummary(orderId);
        boolean hasPartialData = false;
        
        // Try to get payment status
        try {
            String paymentStatus = externalPaymentService.getPaymentStatus(orderId);
            summary.setPaymentStatus(paymentStatus);
        } catch (Exception ex) {
            log.warn("Failed to get payment status for order {}: {}", orderId, ex.getMessage());
            summary.setPaymentStatus("UNKNOWN");
            hasPartialData = true;
        }
        
        // Try to get inventory status
        try {
            String inventoryStatus = externalInventoryService.getReservationStatus(orderId);
            summary.setInventoryStatus(inventoryStatus);
        } catch (Exception ex) {
            log.warn("Failed to get inventory status for order {}: {}", orderId, ex.getMessage());
            summary.setInventoryStatus("UNKNOWN");
            hasPartialData = true;
        }
        
        // Set overall order status based on available data
        if (hasPartialData) {
            summary.setOrderStatus("PARTIAL_DATA");
            summary.setHasPartialData(true);
        } else {
            summary.setOrderStatus("COMPLETE");
        }
        
        return CompletableFuture.completedFuture(summary);
    }

    /**
     * Determine if failure is temporary (network issues, timeouts)
     */
    private boolean isTemporaryFailure(Exception ex) {
        return ex instanceof SocketTimeoutException ||
               ex instanceof ConnectException ||
               (ex instanceof HttpServerErrorException && 
                HttpStatus.valueOf(((HttpServerErrorException) ex).getStatusCode().value()) == HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Determine if failure is provider-specific (gateway issues)
     */
    private boolean isProviderSpecificFailure(Exception ex) {
        if (ex instanceof HttpServerErrorException) {
            HttpStatus status = HttpStatus.valueOf(((HttpServerErrorException) ex).getStatusCode().value());
            return status == HttpStatus.BAD_GATEWAY || 
                   status == HttpStatus.GATEWAY_TIMEOUT;
        }
        
        if (ex instanceof HttpClientErrorException) {
            HttpStatus status = HttpStatus.valueOf(((HttpClientErrorException) ex).getStatusCode().value());
            return status == HttpStatus.TOO_MANY_REQUESTS;
        }
        
        return false;
    }
}