package com.ordersystem.common.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility for extracting client IP addresses from HTTP requests.
 * Handles proxy headers and validates IP format.
 */
public final class IpExtractor {

    private static final List<String> PROXY_HEADERS = List.of(
        "X-Forwarded-For",
        "X-Real-IP",
        "CF-Connecting-IP",
        "True-Client-IP",
        "X-Client-IP"
    );

    // Basic IPv4 pattern (simplified for performance)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // Basic IPv6 pattern (simplified)
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$"
    );

    private IpExtractor() {
        // Utility class
    }

    /**
     * Extracts the client IP address from the HTTP request.
     * Checks proxy headers first, then falls back to remote address.
     *
     * @param request the HTTP request
     * @return the client IP address, or "unknown" if unable to extract
     */
    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Check proxy headers in order of preference
        for (String header : PROXY_HEADERS) {
            String ip = extractFromHeader(request, header);
            if (ip != null) {
                return ip;
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        if (isValidIp(remoteAddr)) {
            return remoteAddr;
        }

        return "unknown";
    }

    /**
     * Extracts IP from a specific header, handling comma-separated lists.
     */
    private static String extractFromHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        // Handle comma-separated list (client, proxy1, proxy2)
        // Take the first valid IP (leftmost = original client)
        if (headerValue.contains(",")) {
            String[] ips = headerValue.split(",");
            for (String ip : ips) {
                String trimmedIp = ip.trim();
                if (isValidIp(trimmedIp)) {
                    return trimmedIp;
                }
            }
        } else {
            String trimmedIp = headerValue.trim();
            if (isValidIp(trimmedIp)) {
                return trimmedIp;
            }
        }

        return null;
    }

    /**
     * Validates if a string is a valid IPv4 or IPv6 address.
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        // Reject common placeholder values
        if ("unknown".equalsIgnoreCase(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            // Allow localhost for testing, but mark it clearly
            return ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1");
        }

        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }

    /**
     * Normalizes an IP address for use as a rate limiting key.
     * For IPv6, this could include compression. For now, just returns the IP.
     */
    public static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim().toLowerCase();
    }
}
