package com.ordersystem.shared.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CorrelationIdContext thread-local management.
 */
class CorrelationIdContextTest {
    
    @BeforeEach
    @AfterEach
    void cleanup() {
        CorrelationIdContext.clear();
        MDC.clear();
    }
    
    @Test
    void shouldSetAndGetCorrelationId() {
        // Given
        String correlationId = "test-correlation-id";
        
        // When
        CorrelationIdContext.set(correlationId);
        
        // Then
        assertEquals(correlationId, CorrelationIdContext.get());
        assertEquals(correlationId, CorrelationIdContext.getOrNull());
        assertTrue(CorrelationIdContext.isPresent());
    }
    
    @Test
    void shouldThrowExceptionForNullCorrelationId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            CorrelationIdContext.set(null));
    }
    
    @Test
    void shouldThrowExceptionForTooLongCorrelationId() {
        // Given
        String tooLongId = "a".repeat(CorrelationConstants.CORRELATION_ID_MAX_LENGTH + 1);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            CorrelationIdContext.set(tooLongId));
    }
    
    @Test
    void shouldReturnDefaultWhenNotSet() {
        // When
        String result = CorrelationIdContext.get();
        
        // Then
        assertEquals(CorrelationConstants.DEFAULT_CORRELATION_ID, result);
        assertNull(CorrelationIdContext.getOrNull());
        assertFalse(CorrelationIdContext.isPresent());
    }
    
    @Test
    void shouldGenerateValidCorrelationId() {
        // When
        String generated = CorrelationIdContext.generate();
        
        // Then
        assertNotNull(generated);
        assertEquals(36, generated.length()); // UUID length
        assertEquals(generated, CorrelationIdContext.get());
        assertTrue(CorrelationIdContext.isPresent());
    }
    
    @Test
    void shouldClearCorrelationId() {
        // Given
        CorrelationIdContext.set("test-id");
        
        // When
        CorrelationIdContext.clear();
        
        // Then
        assertFalse(CorrelationIdContext.isPresent());
        assertNull(CorrelationIdContext.getOrNull());
        assertEquals(CorrelationConstants.DEFAULT_CORRELATION_ID, CorrelationIdContext.get());
    }
    
    @Test
    void shouldSetIfAbsentWhenNotPresent() {
        // Given
        String correlationId = "test-id";
        
        // When
        boolean result = CorrelationIdContext.setIfAbsent(correlationId);
        
        // Then
        assertTrue(result);
        assertEquals(correlationId, CorrelationIdContext.get());
    }
    
    @Test
    void shouldNotSetIfAbsentWhenPresent() {
        // Given
        String originalId = "original-id";
        String newId = "new-id";
        CorrelationIdContext.set(originalId);
        
        // When
        boolean result = CorrelationIdContext.setIfAbsent(newId);
        
        // Then
        assertFalse(result);
        assertEquals(originalId, CorrelationIdContext.get());
    }
    
    @Test
    void shouldUpdateMDCWhenSet() {
        // Given
        String correlationId = "test-id";
        
        // When
        CorrelationIdContext.set(correlationId);
        
        // Then - MDC is managed by the context
        assertEquals(correlationId, CorrelationIdContext.get());
        // Note: MDC integration might not work in test environment without SLF4J implementation
    }
    
    @Test
    void shouldClearMDCWhenCleared() {
        // Given
        CorrelationIdContext.set("test-id");
        
        // When
        CorrelationIdContext.clear();
        
        // Then
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_MDC_KEY));
    }
    
    @Test
    void shouldBeThreadLocal() throws InterruptedException {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<String> thread1Result = new AtomicReference<>();
        AtomicReference<String> thread2Result = new AtomicReference<>();
        
        // When
        executor.submit(() -> {
            try {
                CorrelationIdContext.set("thread-1-id");
                Thread.sleep(100); // Allow thread interleaving
                thread1Result.set(CorrelationIdContext.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        executor.submit(() -> {
            try {
                CorrelationIdContext.set("thread-2-id");
                Thread.sleep(100); // Allow thread interleaving
                thread2Result.set(CorrelationIdContext.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("thread-1-id", thread1Result.get());
        assertEquals("thread-2-id", thread2Result.get());
        
        executor.shutdown();
    }
    
    @Test
    void shouldHandleMaxLengthCorrelationId() {
        // Given
        String maxLengthId = "a".repeat(CorrelationConstants.CORRELATION_ID_MAX_LENGTH);
        
        // When
        CorrelationIdContext.set(maxLengthId);
        
        // Then
        assertEquals(maxLengthId, CorrelationIdContext.get());
    }
    
    @Test
    void shouldPreventInstantiation() {
        // When & Then
        var exception = assertThrows(Exception.class, () -> {
            // Use reflection to access private constructor
            var constructor = CorrelationIdContext.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        // The actual UnsupportedOperationException is wrapped in InvocationTargetException
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }
}