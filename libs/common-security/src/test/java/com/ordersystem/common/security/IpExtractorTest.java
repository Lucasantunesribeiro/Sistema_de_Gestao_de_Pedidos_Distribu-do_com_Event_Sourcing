package com.ordersystem.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class IpExtractorTest {

    @Test
    void testExtractFromXForwardedFor() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1, 172.16.0.1");

        String ip = IpExtractor.extractClientIp(request);

        assertEquals("192.168.1.100", ip, "Should extract first IP from X-Forwarded-For");
    }

    @Test
    void testExtractFromXRealIp() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.45");

        String ip = IpExtractor.extractClientIp(request);

        assertEquals("203.0.113.45", ip, "Should extract IP from X-Real-IP");
    }

    @Test
    void testExtractFromRemoteAddr() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("198.51.100.10");

        String ip = IpExtractor.extractClientIp(request);

        assertEquals("198.51.100.10", ip, "Should fallback to remote address");
    }

    @Test
    void testExtractIPv6() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8:85a3::8a2e:370:7334");

        String ip = IpExtractor.extractClientIp(request);

        assertEquals("2001:db8:85a3::8a2e:370:7334", ip, "Should extract IPv6 address");
    }

    @Test
    void testExtractWithInvalidIP() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("invalid-ip");
        when(request.getRemoteAddr()).thenReturn("unknown");

        String ip = IpExtractor.extractClientIp(request);

        assertEquals("unknown", ip, "Should return unknown for invalid IP");
    }

    @Test
    void testNormalizeIp() {
        assertEquals("192.168.1.1", IpExtractor.normalizeIp("192.168.1.1"));
        assertEquals("2001:db8::1", IpExtractor.normalizeIp("2001:DB8::1"));
        assertEquals("unknown", IpExtractor.normalizeIp(null));
        assertEquals("unknown", IpExtractor.normalizeIp(""));
    }

    @Test
    void testExtractWithNullRequest() {
        String ip = IpExtractor.extractClientIp(null);
        assertEquals("unknown", ip, "Should return unknown for null request");
    }

    @Test
    void testExtractWithMultipleHeaders() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.50");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        String ip = IpExtractor.extractClientIp(request);

        // Should check headers in order of preference
        assertNotNull(ip);
        assertTrue(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"));
    }
}
