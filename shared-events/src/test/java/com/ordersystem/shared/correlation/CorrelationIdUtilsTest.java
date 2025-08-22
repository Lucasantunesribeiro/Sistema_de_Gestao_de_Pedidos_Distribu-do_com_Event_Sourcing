package com.ordersystem.shared.correlation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CorrelationIdUtils utility methods.
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdUtilsTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private MessageProperties messageProperties;
    
    @BeforeEach
    @AfterEach
    void cleanup() {
        CorrelationIdContext.clear();
    }
    
    @Test
    void shouldExtractCorrelationIdFromRequest() {
        // Given
        String correlationId = "test-correlation-id";
        when(request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER)).thenReturn(correlationId);
        
        // When
        String result = CorrelationIdUtils.extractFromRequest(request);
        
        // Then
        assertEquals(correlationId, result);
    }
    
    @Test
    void shouldReturnNullWhenRequestHeaderEmpty() {
        // Given
        when(request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER)).thenReturn("");
        
        // When
        String result = CorrelationIdUtils.extractFromRequest(request);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void shouldReturnNullWhenRequestHeaderNull() {
        // Given
        when(request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER)).thenReturn(null);
        
        // When
        String result = CorrelationIdUtils.extractFromRequest(request);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void shouldReturnNullWhenRequestIsNull() {
        // When
        String result = CorrelationIdUtils.extractFromRequest(null);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void shouldExtractCorrelationIdFromHeaders() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        String correlationId = "test-correlation-id";
        headers.set(CorrelationConstants.CORRELATION_ID_HEADER, correlationId);
        
        // When
        String result = CorrelationIdUtils.extractFromHeaders(headers);
        
        // Then
        assertEquals(correlationId, result);
    }
    
    @Test
    void shouldReturnNullWhenHeadersNull() {
        // When
        String result = CorrelationIdUtils.extractFromHeaders(null);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void shouldInjectCorrelationIdIntoResponse() {
        // Given
        String correlationId = "test-correlation-id";
        CorrelationIdContext.set(correlationId);
        
        // When
        CorrelationIdUtils.injectIntoResponse(response);
        
        // Then
        verify(response).setHeader(CorrelationConstants.CORRELATION_ID_HEADER, correlationId);
    }
    
    @Test
    void shouldNotInjectWhenNoCorrelationIdInContext() {
        // When
        CorrelationIdUtils.injectIntoResponse(response);
        
        // Then
        verify(response, never()).setHeader(anyString(), anyString());
    }
    
    @Test
    void shouldNotFailWhenResponseIsNull() {
        // Given
        CorrelationIdContext.set("test-id");
        
        // When & Then (should not throw)
        assertDoesNotThrow(() -> CorrelationIdUtils.injectIntoResponse(null));
    }
    
    @Test
    void shouldInjectCorrelationIdIntoHeaders() {
        // Given
        String correlationId = "test-correlation-id";
        CorrelationIdContext.set(correlationId);
        HttpHeaders headers = new HttpHeaders();
        
        // When
        CorrelationIdUtils.injectIntoHeaders(headers);
        
        // Then
        assertEquals(correlationId, headers.getFirst(CorrelationConstants.CORRELATION_ID_HEADER));
    }
    
    @Test
    void shouldExtractCorrelationIdFromMessage() {
        // Given
        String correlationId = "test-correlation-id";
        Map<String, Object> headers = new HashMap<>();
        headers.put(CorrelationConstants.CORRELATION_ID_MESSAGE_PROPERTY, correlationId);
        when(messageProperties.getHeaders()).thenReturn(headers);
        
        // When
        String result = CorrelationIdUtils.extractFromMessage(messageProperties);
        
        // Then
        assertEquals(correlationId, result);
    }
    
    @Test
    void shouldReturnNullWhenMessagePropertiesNull() {
        // When
        String result = CorrelationIdUtils.extractFromMessage(null);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void shouldReturnNullWhenMessageHeadersNull() {
        // Given
        when(messageProperties.getHeaders()).thenReturn(null);
        
        // When
        String result = CorrelationIdUtils.extractFromMessage(messageProperties);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void shouldInjectCorrelationIdIntoMessage() {
        // Given
        String correlationId = "test-correlation-id";
        CorrelationIdContext.set(correlationId);
        Map<String, Object> headers = new HashMap<>();
        when(messageProperties.getHeaders()).thenReturn(headers);
        
        // When
        CorrelationIdUtils.injectIntoMessage(messageProperties);
        
        // Then
        assertEquals(correlationId, headers.get(CorrelationConstants.CORRELATION_ID_MESSAGE_PROPERTY));
    }
    
    @Test
    void shouldCreateSqlComment() {
        // Given
        String correlationId = "test-correlation-id";
        CorrelationIdContext.set(correlationId);
        
        // When
        String result = CorrelationIdUtils.createSqlComment();
        
        // Then
        assertEquals("/* correlationId: test-correlation-id */", result);
    }
    
    @Test
    void shouldReturnEmptyStringWhenNoCorrelationIdForSqlComment() {
        // When
        String result = CorrelationIdUtils.createSqlComment();
        
        // Then
        assertEquals("", result);
    }
    
    @Test
    void shouldValidateCorrelationId() {
        // Valid cases
        assertTrue(CorrelationIdUtils.isValidCorrelationId("valid-id"));
        assertTrue(CorrelationIdUtils.isValidCorrelationId("a".repeat(36)));
        
        // Invalid cases
        assertFalse(CorrelationIdUtils.isValidCorrelationId(null));
        assertFalse(CorrelationIdUtils.isValidCorrelationId(""));
        assertFalse(CorrelationIdUtils.isValidCorrelationId("   "));
        assertFalse(CorrelationIdUtils.isValidCorrelationId("a".repeat(37)));
    }
    
    @Test
    void shouldGenerateValidCorrelationId() {
        // When
        String result = CorrelationIdUtils.generateCorrelationId();
        
        // Then
        assertNotNull(result);
        assertEquals(36, result.length()); // UUID length
        assertTrue(CorrelationIdUtils.isValidCorrelationId(result));
    }
    
    @Test
    void shouldEnsureCorrelationIdWithValidInput() {
        // Given
        String existingId = "existing-id";
        
        // When
        String result = CorrelationIdUtils.ensureCorrelationId(existingId);
        
        // Then
        assertEquals(existingId, result);
    }
    
    @Test
    void shouldEnsureCorrelationIdWithInvalidInput() {
        // When
        String result = CorrelationIdUtils.ensureCorrelationId(null);
        
        // Then
        assertNotNull(result);
        assertEquals(36, result.length()); // UUID length
        assertTrue(CorrelationIdUtils.isValidCorrelationId(result));
    }
    
    @Test
    void shouldPreventInstantiation() {
        // When & Then
        var exception = assertThrows(Exception.class, () -> {
            // Use reflection to access private constructor
            var constructor = CorrelationIdUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        // The actual UnsupportedOperationException is wrapped in InvocationTargetException
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }
}