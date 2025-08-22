package com.ordersystem.shared.correlation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for correlation ID end-to-end flow.
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdIntegrationTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private CorrelationIdInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        CorrelationIdContext.clear();
        interceptor = new CorrelationIdInterceptor();
    }
    
    @Test
    void shouldHandleCompleteRequestResponseFlow() throws Exception {
        // Given - Incoming request with correlation ID
        String incomingCorrelationId = "incoming-correlation-id";
        when(request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER))
            .thenReturn(incomingCorrelationId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/orders");
        
        // When - Process request through interceptor
        boolean preHandleResult = interceptor.preHandle(request, response, null);
        
        // Then - Verify correlation ID is set
        assertTrue(preHandleResult);
        assertEquals(incomingCorrelationId, CorrelationIdContext.get());
        verify(response).setHeader(CorrelationConstants.CORRELATION_ID_HEADER, incomingCorrelationId);
        
        // Simulate business logic
        String businessCorrelationId = CorrelationIdContext.get();
        assertEquals(incomingCorrelationId, businessCorrelationId);
        
        // Clean up
        interceptor.afterCompletion(request, response, null, null);
        assertEquals(CorrelationConstants.DEFAULT_CORRELATION_ID, CorrelationIdContext.get());
    }
    
    @Test
    void shouldGenerateCorrelationIdWhenNoneProvided() throws Exception {
        // Given - Request without correlation ID
        when(request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER)).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/orders");
        
        // When
        interceptor.preHandle(request, response, null);
        
        // Then
        assertTrue(CorrelationIdContext.isPresent());
        assertNotEquals(CorrelationConstants.DEFAULT_CORRELATION_ID, CorrelationIdContext.get());
        assertEquals(36, CorrelationIdContext.get().length()); // UUID length
        
        // Verify response header is set
        verify(response).setHeader(eq(CorrelationConstants.CORRELATION_ID_HEADER), anyString());
    }
    
    @Test
    void shouldHandleMessageFlowWithCorrelation() {
        // Given - Set correlation ID in context
        String correlationId = "message-correlation-id";
        CorrelationIdContext.set(correlationId);
        
        // Create RabbitMQ message properties
        MessageProperties properties = new MessageProperties();
        
        // When - Inject correlation into message
        CorrelationIdUtils.injectIntoMessage(properties);
        
        // Then - Verify correlation is in message
        assertEquals(correlationId, 
            properties.getHeaders().get(CorrelationConstants.CORRELATION_ID_MESSAGE_PROPERTY));
        
        // Simulate message consumption
        CorrelationIdContext.clear();
        assertFalse(CorrelationIdContext.isPresent());
        
        // Extract correlation from message
        String extractedCorrelationId = CorrelationIdUtils.extractFromMessage(properties);
        assertEquals(correlationId, extractedCorrelationId);
        
        // Set extracted correlation in context
        CorrelationIdContext.set(extractedCorrelationId);
        assertEquals(correlationId, CorrelationIdContext.get());
    }
    
    @Test
    void shouldMaintainCorrelationAcrossAsyncOperations() throws ExecutionException, InterruptedException {
        // Given - Set correlation ID
        String originalCorrelationId = "async-correlation-id";
        CorrelationIdContext.set(originalCorrelationId);
        
        // When - Execute async operation
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // Manually propagate correlation to async thread
            CorrelationIdContext.set(originalCorrelationId);
            
            try {
                // Simulate async work
                Thread.sleep(100);
                return CorrelationIdContext.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                CorrelationIdContext.clear();
            }
        });
        
        // Then - Verify correlation is maintained
        String asyncCorrelationId = future.get();
        assertEquals(originalCorrelationId, asyncCorrelationId);
        
        // Original thread correlation should still be present
        assertEquals(originalCorrelationId, CorrelationIdContext.get());
    }
    
    @Test
    void shouldHandleCorrelationIdValidation() {
        // Valid correlation IDs
        assertTrue(CorrelationIdUtils.isValidCorrelationId("valid-id"));
        assertTrue(CorrelationIdUtils.isValidCorrelationId("1234567890"));
        assertTrue(CorrelationIdUtils.isValidCorrelationId("a".repeat(36)));
        
        // Invalid correlation IDs
        assertFalse(CorrelationIdUtils.isValidCorrelationId(null));
        assertFalse(CorrelationIdUtils.isValidCorrelationId(""));
        assertFalse(CorrelationIdUtils.isValidCorrelationId("   "));
        assertFalse(CorrelationIdUtils.isValidCorrelationId("a".repeat(37)));
    }
    
    @Test
    void shouldHandleCorrelationIdEnsurement() {
        // Ensure with valid ID
        String validId = "valid-id";
        assertEquals(validId, CorrelationIdUtils.ensureCorrelationId(validId));
        
        // Ensure with invalid ID
        String ensuredId = CorrelationIdUtils.ensureCorrelationId(null);
        assertNotNull(ensuredId);
        assertEquals(36, ensuredId.length());
        assertTrue(CorrelationIdUtils.isValidCorrelationId(ensuredId));
        
        // Ensure with empty ID
        String ensuredEmptyId = CorrelationIdUtils.ensureCorrelationId("");
        assertNotNull(ensuredEmptyId);
        assertEquals(36, ensuredEmptyId.length());
        assertTrue(CorrelationIdUtils.isValidCorrelationId(ensuredEmptyId));
    }
    
    @Test
    void shouldCreateSqlCommentWithCorrelation() {
        // Given - Set correlation ID
        String correlationId = "sql-correlation-id";
        CorrelationIdContext.set(correlationId);
        
        // When - Create SQL comment
        String sqlComment = CorrelationIdUtils.createSqlComment();
        
        // Then - Verify comment format
        assertEquals("/* correlationId: sql-correlation-id */", sqlComment);
        
        // Test without correlation
        CorrelationIdContext.clear();
        String emptySqlComment = CorrelationIdUtils.createSqlComment();
        assertEquals("", emptySqlComment);
    }
    
    @Test
    void shouldHandleInterceptorExceptionGracefully() throws Exception {
        // Given - Request that will cause extraction to fail
        when(request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER))
            .thenThrow(new RuntimeException("Header extraction failed"));
        
        // When - Interceptor handles exception
        boolean result = interceptor.preHandle(request, response, null);
        
        // Then - Should still succeed with fallback correlation ID
        assertTrue(result);
        assertTrue(CorrelationIdContext.isPresent());
        assertNotNull(CorrelationIdContext.getOrNull());
        
        // Should still set response header
        verify(response).setHeader(eq(CorrelationConstants.CORRELATION_ID_HEADER), anyString());
    }
}