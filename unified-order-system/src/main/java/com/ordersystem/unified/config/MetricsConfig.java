package com.ordersystem.unified.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for custom business metrics.
 * Provides comprehensive observability for order, payment, and inventory operations.
 */
@Configuration
public class MetricsConfig {

    // Order Metrics
    @Bean
    public Counter orderCreationCounter(MeterRegistry registry) {
        return Counter.builder("order.creation.total")
                .description("Total number of order creation attempts")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter orderCreationSuccessCounter(MeterRegistry registry) {
        return Counter.builder("order.creation.success")
                .description("Total number of successful order creations")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter orderCreationFailedCounter(MeterRegistry registry) {
        return Counter.builder("order.creation.failed")
                .description("Total number of failed order creations")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter orderCancellationCounter(MeterRegistry registry) {
        return Counter.builder("order.cancellation.total")
                .description("Total number of order cancellations")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Timer orderCreationTimer(MeterRegistry registry) {
        return Timer.builder("order.creation.duration")
                .description("Time taken to create an order")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    // Payment Metrics
    @Bean
    public Counter paymentAttemptsCounter(MeterRegistry registry) {
        return Counter.builder("payment.attempts.total")
                .description("Total number of payment attempts")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter paymentSuccessCounter(MeterRegistry registry) {
        return Counter.builder("payment.success.total")
                .description("Total number of successful payments")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter paymentFailedCounter(MeterRegistry registry) {
        return Counter.builder("payment.failed.total")
                .description("Total number of failed payments")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Timer paymentProcessingTimer(MeterRegistry registry) {
        return Timer.builder("payment.processing.duration")
                .description("Time taken to process a payment")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    // Inventory Metrics
    @Bean
    public Counter inventoryReservationCounter(MeterRegistry registry) {
        return Counter.builder("inventory.reservation.total")
                .description("Total number of inventory reservation attempts")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter inventoryReservationSuccessCounter(MeterRegistry registry) {
        return Counter.builder("inventory.reservation.success")
                .description("Total number of successful inventory reservations")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Counter inventoryReservationFailedCounter(MeterRegistry registry) {
        return Counter.builder("inventory.reservation.failed")
                .description("Total number of failed inventory reservations")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    @Bean
    public Timer inventoryReservationTimer(MeterRegistry registry) {
        return Timer.builder("inventory.reservation.duration")
                .description("Time taken to reserve inventory")
                .tag("application", "unified-order-system")
                .register(registry);
    }

    // Database Connection Pool Metrics (HikariCP is auto-configured)
    // JVM Metrics
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    @Bean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }
}
