package com.ordersystem.order.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom metrics for Order Service
 * Tracks business and technical metrics for monitoring and alerting
 */
@Component
public class OrderMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter ordersCreatedCounter;
    private final Counter ordersFailedCounter;
    private final Counter paymentsProcessedCounter;
    private final Counter paymentsFailedCounter;
    private final Counter inventoryChecksCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaFailedCounter;
    
    // Timers
    private final Timer orderCreationTimer;
    private final Timer paymentProcessingTimer;
    private final Timer inventoryCheckTimer;
    private final Timer sagaExecutionTimer;
    
    // Gauges
    private final AtomicInteger activeSagasGauge;
    private final AtomicInteger pendingOrdersGauge;
    
    // Histograms
    private final DistributionSummary orderValueDistribution;
    
    @Autowired
    public OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.ordersFailedCounter = Counter.builder("orders.failed.total")
                .description("Total number of failed order creations")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.paymentsProcessedCounter = Counter.builder("payments.processed.total")
                .description("Total number of payments processed")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.paymentsFailedCounter = Counter.builder("payments.failed.total")
                .description("Total number of failed payments")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.inventoryChecksCounter = Counter.builder("inventory.checks.total")
                .description("Total number of inventory checks")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.sagaCompletedCounter = Counter.builder("sagas.completed.total")
                .description("Total number of completed sagas")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.sagaFailedCounter = Counter.builder("sagas.failed.total")
                .description("Total number of failed sagas")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Initialize timers
        this.orderCreationTimer = Timer.builder("orders.creation.duration")
                .description("Time taken to create an order")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.paymentProcessingTimer = Timer.builder("payments.processing.duration")
                .description("Time taken to process a payment")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.inventoryCheckTimer = Timer.builder("inventory.check.duration")
                .description("Time taken to check inventory")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.sagaExecutionTimer = Timer.builder("saga.execution.duration")
                .description("Time taken to execute a complete saga")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Initialize gauges
        this.activeSagasGauge = new AtomicInteger(0);
        Gauge.builder("sagas.active.current", activeSagasGauge, AtomicInteger::get)
                .description("Current number of active sagas")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.pendingOrdersGauge = new AtomicInteger(0);
        Gauge.builder("orders.pending.current", pendingOrdersGauge, AtomicInteger::get)
                .description("Current number of pending orders")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Initialize distribution summary
        this.orderValueDistribution = DistributionSummary.builder("orders.value.distribution")
                .description("Distribution of order values")
                .tag("service", "order-service")
                .baseUnit("currency")
                .register(meterRegistry);
    }
    
    // Counter methods
    public void incrementOrdersCreated() {
        ordersCreatedCounter.increment();
    }
    
    public void incrementOrdersCreated(String customerType) {
        Counter.builder("orders.created.total")
                .tag("customer.type", customerType)
                .tag("service", "order-service")
                .register(meterRegistry)
                .increment();
    }
    
    public void incrementOrdersFailed(String reason) {
        ordersFailedCounter.increment();
    }
    
    public void incrementPaymentsProcessed(String paymentMethod) {
        paymentsProcessedCounter.increment();
    }
    
    public void incrementPaymentsFailed(String reason) {
        paymentsFailedCounter.increment();
    }
    
    public void incrementInventoryChecks() {
        inventoryChecksCounter.increment();
    }
    
    public void incrementSagaCompleted() {
        sagaCompletedCounter.increment();
    }
    
    public void incrementSagaFailed(String reason) {
        sagaFailedCounter.increment();
    }
    
    // Timer methods
    public Timer.Sample startOrderCreationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordOrderCreationTime(Timer.Sample sample) {
        sample.stop(orderCreationTimer);
    }
    
    public void recordOrderCreationTime(long duration, TimeUnit unit) {
        orderCreationTimer.record(duration, unit);
    }
    
    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordPaymentProcessingTime(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }
    
    public Timer.Sample startInventoryCheckTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordInventoryCheckTime(Timer.Sample sample) {
        sample.stop(inventoryCheckTimer);
    }
    
    public Timer.Sample startSagaExecutionTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordSagaExecutionTime(Timer.Sample sample) {
        sample.stop(sagaExecutionTimer);
    }
    
    // Gauge methods
    public void setActiveSagas(int count) {
        activeSagasGauge.set(count);
    }
    
    public void incrementActiveSagas() {
        activeSagasGauge.incrementAndGet();
    }
    
    public void decrementActiveSagas() {
        activeSagasGauge.decrementAndGet();
    }
    
    public void setPendingOrders(int count) {
        pendingOrdersGauge.set(count);
    }
    
    public void incrementPendingOrders() {
        pendingOrdersGauge.incrementAndGet();
    }
    
    public void decrementPendingOrders() {
        pendingOrdersGauge.decrementAndGet();
    }
    
    // Distribution methods
    public void recordOrderValue(double value) {
        orderValueDistribution.record(value);
    }
    
    // Custom business metrics
    public void recordCustomerOrderFrequency(String customerId) {
        Counter.builder("customer.order.frequency")
                .description("Frequency of orders per customer")
                .tag("customer.id", customerId)
                .tag("service", "order-service")
                .register(meterRegistry)
                .increment();
    }
    
    public void recordProductPopularity(String productId) {
        Counter.builder("product.popularity")
                .description("Popularity of products based on orders")
                .tag("product.id", productId)
                .tag("service", "order-service")
                .register(meterRegistry)
                .increment();
    }
    
    // Health metrics
    public void recordHealthCheckDuration(String dependency, long duration) {
        Timer.builder("health.check.duration")
                .description("Duration of health checks")
                .tag("dependency", dependency)
                .tag("service", "order-service")
                .register(meterRegistry)
                .record(duration, TimeUnit.MILLISECONDS);
    }
    
    public void recordCircuitBreakerState(String circuitBreakerName, String state) {
        Gauge.builder("circuit.breaker.state", this, metrics -> getCircuitBreakerStateValue(state))
                .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
                .tag("circuit.breaker", circuitBreakerName)
                .tag("service", "order-service")
                .register(meterRegistry);
    }
    
    private double getCircuitBreakerStateValue(String state) {
        return switch (state.toUpperCase()) {
            case "CLOSED" -> 0.0;
            case "OPEN" -> 1.0;
            case "HALF_OPEN" -> 2.0;
            default -> -1.0;
        };
    }
}