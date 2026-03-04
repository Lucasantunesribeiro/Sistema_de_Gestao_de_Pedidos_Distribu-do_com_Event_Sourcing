package com.ordersystem.unified.shared.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafeEnumParserTest {

    private enum TestEnum {
        VALUE_ONE,
        VALUE_TWO
    }

    @Test
    void parseEnumOrThrow_ValidValue_ReturnsEnum() {
        TestEnum result = SafeEnumParser.parseEnumOrThrow(TestEnum.class, "VALUE_ONE", "testField");
        assertEquals(TestEnum.VALUE_ONE, result);
    }

    @Test
    void parseEnumOrThrow_LowerCaseValue_ReturnsEnum() {
        TestEnum result = SafeEnumParser.parseEnumOrThrow(TestEnum.class, "value_two", "testField");
        assertEquals(TestEnum.VALUE_TWO, result);
    }

    @Test
    void parseEnumOrThrow_NullValue_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            SafeEnumParser.parseEnumOrThrow(TestEnum.class, null, "testField");
        });
        assertTrue(exception.getMessage().contains("testField cannot be null or empty"));
    }

    @Test
    void parseEnumOrThrow_EmptyValue_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            SafeEnumParser.parseEnumOrThrow(TestEnum.class, "  ", "testField");
        });
        assertTrue(exception.getMessage().contains("testField cannot be null or empty"));
    }

    @Test
    void parseEnumOrThrow_InvalidValue_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            SafeEnumParser.parseEnumOrThrow(TestEnum.class, "INVALID_VALUE", "testField");
        });
        assertTrue(exception.getMessage().contains("Invalid value for testField: INVALID_VALUE"));
    }
}
