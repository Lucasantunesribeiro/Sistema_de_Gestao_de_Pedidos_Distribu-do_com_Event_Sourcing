package com.ordersystem.common.messaging;

import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.MDC;

public final class CorrelationId {
    private CorrelationId() {
    }

    public static String resolve(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return headerValue;
    }

    public static void withMdc(String correlationId, Runnable action) {
        MDC.put("correlationId", correlationId);
        try {
            action.run();
        } finally {
            MDC.remove("correlationId");
        }
    }

    public static <T> T withMdc(String correlationId, Supplier<T> action) {
        MDC.put("correlationId", correlationId);
        try {
            return action.get();
        } finally {
            MDC.remove("correlationId");
        }
    }
}
