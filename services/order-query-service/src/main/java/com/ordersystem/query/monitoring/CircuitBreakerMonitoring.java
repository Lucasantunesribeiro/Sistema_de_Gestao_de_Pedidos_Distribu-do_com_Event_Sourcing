package com.ordersystem.query.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Simplified Circuit Breaker Monitoring without Micrometer dependencies
 * Provides basic monitoring and alerting capabilities
 */
@Component
public class CircuitBreakerMonitoring {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerMonitoring.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, CircuitBreakerStats> stats = new ConcurrentHashMap<>();

    @Autowired
    public CircuitBreakerMonitoring(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing circuit breaker monitoring");
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::bindCircuitBreakerEvents);
        
        // Listen for new circuit breakers
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(event -> bindCircuitBreakerEvents(event.getAddedEntry()));
    }

    private void bindCircuitBreakerEvents(CircuitBreaker circuitBreaker) {
        String name = circuitBreaker.getName();
        log.debug("Binding events for circuit breaker: {}", name);
        
        stats.put(name, new CircuitBreakerStats(name));

        circuitBreaker.getEventPublisher()
            .onSuccess(event -> {
                log.debug("Circuit breaker {} - successful call", name);
                stats.get(name).incrementSuccess();
            })
            .onError(event -> {
                log.debug("Circuit breaker {} - failed call: {}", name, event.getThrowable().getMessage());
                stats.get(name).incrementFailure();
            })
            .onCallNotPermitted(event -> {
                log.debug("Circuit breaker {} - call not permitted", name);
                stats.get(name).incrementNotPermitted();
            })
            .onSlowCallRateExceeded(event -> {
                log.warn("Circuit breaker '{}' slow call rate exceeded: {}%", name, event.getSlowCallRate());
                stats.get(name).recordSlowCallRateExceeded();
            })
            .onFailureRateExceeded(event -> {
                log.warn("Circuit breaker '{}' failure rate exceeded: {}%", name, event.getFailureRate());
                stats.get(name).recordFailureRateExceeded();
            });
    }

    /**
     * Handle circuit breaker state transition events
     */
    @EventListener
    public void handleCircuitBreakerStateChange(CircuitBreakerOnStateTransitionEvent event) {
        String name = event.getCircuitBreakerName();
        CircuitBreaker.StateTransition transition = event.getStateTransition();
        
        log.info("Circuit breaker '{}' changed from {} to {}", 
            name, transition.getFromState(), transition.getToState());

        CircuitBreakerStats circuitStats = stats.get(name);
        if (circuitStats != null) {
            circuitStats.recordStateTransition(transition.getFromState(), transition.getToState());
        }

        // Alert on critical state changes
        if (transition.getToState() == CircuitBreaker.State.OPEN) {
            log.error("ALERT: Circuit breaker '{}' opened due to failures", name);
        } else if (transition.getToState() == CircuitBreaker.State.CLOSED && 
                  transition.getFromState() == CircuitBreaker.State.HALF_OPEN) {
            log.info("Circuit breaker '{}' recovered and closed", name);
        }
    }

    /**
     * Get circuit breaker statistics
     */
    public Map<String, CircuitBreakerStats> getAllStats() {
        return Map.copyOf(stats);
    }

    /**
     * Get circuit breaker health summary
     */
    public HealthSummary getHealthSummary() {
        HealthSummary summary = new HealthSummary();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            String name = cb.getName();
            CircuitBreaker.State state = cb.getState();
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            
            HealthInfo info = new HealthInfo();
            info.setName(name);
            info.setState(state.toString());
            info.setFailureRate(metrics.getFailureRate());
            info.setSlowCallRate(metrics.getSlowCallRate());
            info.setSuccessfulCalls(metrics.getNumberOfSuccessfulCalls());
            info.setFailedCalls(metrics.getNumberOfFailedCalls());
            info.setNotPermittedCalls(metrics.getNumberOfNotPermittedCalls());
            
            summary.addCircuitBreaker(info);
            
            if (state == CircuitBreaker.State.OPEN) {
                summary.incrementOpenCircuits();
            }
        });
        
        return summary;
    }

    /**
     * Circuit breaker statistics
     */
    public static class CircuitBreakerStats {
        private final String name;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong notPermittedCount = new AtomicLong(0);
        private final AtomicLong slowCallRateExceededCount = new AtomicLong(0);
        private final AtomicLong failureRateExceededCount = new AtomicLong(0);
        private final AtomicLong stateTransitionCount = new AtomicLong(0);
        private final long createdAt = System.currentTimeMillis();
        
        public CircuitBreakerStats(String name) {
            this.name = name;
        }
        
        public void incrementSuccess() { successCount.incrementAndGet(); }
        public void incrementFailure() { failureCount.incrementAndGet(); }
        public void incrementNotPermitted() { notPermittedCount.incrementAndGet(); }
        public void recordSlowCallRateExceeded() { slowCallRateExceededCount.incrementAndGet(); }
        public void recordFailureRateExceeded() { failureRateExceededCount.incrementAndGet(); }
        public void recordStateTransition(CircuitBreaker.State from, CircuitBreaker.State to) { 
            stateTransitionCount.incrementAndGet(); 
        }
        
        // Getters
        public String getName() { return name; }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public long getNotPermittedCount() { return notPermittedCount.get(); }
        public long getSlowCallRateExceededCount() { return slowCallRateExceededCount.get(); }
        public long getFailureRateExceededCount() { return failureRateExceededCount.get(); }
        public long getStateTransitionCount() { return stateTransitionCount.get(); }
        public long getCreatedAt() { return createdAt; }
        
        public double getSuccessRate() {
            long total = successCount.get() + failureCount.get();
            return total == 0 ? 0 : (double) successCount.get() / total * 100;
        }
    }

    /**
     * Health summary for dashboard
     */
    public static class HealthSummary {
        private int totalCircuitBreakers = 0;
        private int openCircuits = 0;
        private java.util.List<HealthInfo> circuitBreakers = new java.util.ArrayList<>();
        
        public void addCircuitBreaker(HealthInfo info) {
            circuitBreakers.add(info);
            totalCircuitBreakers++;
        }
        
        public void incrementOpenCircuits() {
            openCircuits++;
        }
        
        public boolean isHealthy() {
            return openCircuits == 0;
        }
        
        // Getters
        public int getTotalCircuitBreakers() { return totalCircuitBreakers; }
        public int getOpenCircuits() { return openCircuits; }
        public java.util.List<HealthInfo> getCircuitBreakers() { return circuitBreakers; }
    }

    /**
     * Individual circuit breaker health info
     */
    public static class HealthInfo {
        private String name;
        private String state;
        private float failureRate;
        private float slowCallRate;
        private long successfulCalls;
        private long failedCalls;
        private long notPermittedCalls;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public float getFailureRate() { return failureRate; }
        public void setFailureRate(float failureRate) { this.failureRate = failureRate; }
        public float getSlowCallRate() { return slowCallRate; }
        public void setSlowCallRate(float slowCallRate) { this.slowCallRate = slowCallRate; }
        public long getSuccessfulCalls() { return successfulCalls; }
        public void setSuccessfulCalls(long successfulCalls) { this.successfulCalls = successfulCalls; }
        public long getFailedCalls() { return failedCalls; }
        public void setFailedCalls(long failedCalls) { this.failedCalls = failedCalls; }
        public long getNotPermittedCalls() { return notPermittedCalls; }
        public void setNotPermittedCalls(long notPermittedCalls) { this.notPermittedCalls = notPermittedCalls; }
    }
}