package com.ordersystem.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeEnumParserTest {

    enum TestStatus {
        PENDING,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    @Test
    void testParseEnumOrThrow_ValidValue() {
        TestStatus result = SafeEnumParser.parseEnumOrThrow(TestStatus.class, "ACTIVE", "status");
        assertEquals(TestStatus.ACTIVE, result);
    }

    @Test
    void testParseEnumOrThrow_LowercaseValue() {
        TestStatus result = SafeEnumParser.parseEnumOrThrow(TestStatus.class, "pending", "status");
        assertEquals(TestStatus.PENDING, result, "Should handle lowercase by converting to uppercase");
    }

    @Test
    void testParseEnumOrThrow_MixedCaseValue() {
        TestStatus result = SafeEnumParser.parseEnumOrThrow(TestStatus.class, "CoMpLeTeD", "status");
        assertEquals(TestStatus.COMPLETED, result, "Should handle mixed case");
    }

    @Test
    void testParseEnumOrThrow_InvalidValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            SafeEnumParser.parseEnumOrThrow(TestStatus.class, "INVALID", "status")
        );

        assertTrue(exception.getMessage().contains("Invalid status: 'INVALID'"));
        assertTrue(exception.getMessage().contains("Valid values:"));
        assertTrue(exception.getMessage().contains("PENDING"));
        assertTrue(exception.getMessage().contains("ACTIVE"));
    }

    @Test
    void testParseEnumOrThrow_NullValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            SafeEnumParser.parseEnumOrThrow(TestStatus.class, null, "status")
        );

        assertEquals("status is required", exception.getMessage());
    }

    @Test
    void testParseEnumOrThrow_BlankValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            SafeEnumParser.parseEnumOrThrow(TestStatus.class, "   ", "status")
        );

        assertEquals("status is required", exception.getMessage());
    }

    @Test
    void testParseEnumOrNull_ValidValue() {
        TestStatus result = SafeEnumParser.parseEnumOrNull(TestStatus.class, "CANCELLED");
        assertEquals(TestStatus.CANCELLED, result);
    }

    @Test
    void testParseEnumOrNull_InvalidValue() {
        TestStatus result = SafeEnumParser.parseEnumOrNull(TestStatus.class, "INVALID");
        assertNull(result, "Should return null for invalid value");
    }

    @Test
    void testParseEnumOrNull_NullValue() {
        TestStatus result = SafeEnumParser.parseEnumOrNull(TestStatus.class, null);
        assertNull(result, "Should return null for null value");
    }

    @Test
    void testParseEnumOrDefault_ValidValue() {
        TestStatus result = SafeEnumParser.parseEnumOrDefault(TestStatus.class, "ACTIVE", TestStatus.PENDING);
        assertEquals(TestStatus.ACTIVE, result);
    }

    @Test
    void testParseEnumOrDefault_InvalidValue() {
        TestStatus result = SafeEnumParser.parseEnumOrDefault(TestStatus.class, "INVALID", TestStatus.PENDING);
        assertEquals(TestStatus.PENDING, result, "Should return default for invalid value");
    }

    @Test
    void testParseEnumOrDefault_NullValue() {
        TestStatus result = SafeEnumParser.parseEnumOrDefault(TestStatus.class, null, TestStatus.PENDING);
        assertEquals(TestStatus.PENDING, result, "Should return default for null value");
    }
}
