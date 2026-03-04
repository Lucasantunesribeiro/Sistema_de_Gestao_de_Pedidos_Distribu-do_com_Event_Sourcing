package com.ordersystem.common.security;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility for safely parsing enum values from strings.
 * Provides helpful error messages instead of throwing generic exceptions.
 */
public final class SafeEnumParser {

    private SafeEnumParser() {
        // Utility class
    }

    /**
     * Safely parses an enum value from a string.
     * Throws IllegalArgumentException with helpful error message on invalid input.
     *
     * @param enumClass the enum class
     * @param value     the string value to parse
     * @param fieldName the field name (for error messages)
     * @param <E>       the enum type
     * @return the parsed enum value
     * @throws IllegalArgumentException if value is null, blank, or invalid
     */
    public static <E extends Enum<E>> E parseEnumOrThrow(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            String validValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                String.format("Invalid %s: '%s'. Valid values: %s", fieldName, value, validValues)
            );
        }
    }

    /**
     * Safely parses an enum value from a string, returning null if invalid.
     * Does not throw exceptions.
     *
     * @param enumClass the enum class
     * @param value     the string value to parse
     * @param <E>       the enum type
     * @return the parsed enum value, or null if invalid
     */
    public static <E extends Enum<E>> E parseEnumOrNull(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Safely parses an enum value from a string, returning a default value if invalid.
     *
     * @param enumClass    the enum class
     * @param value        the string value to parse
     * @param defaultValue the default value to return if parsing fails
     * @param <E>          the enum type
     * @return the parsed enum value, or defaultValue if invalid
     */
    public static <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E defaultValue) {
        E result = parseEnumOrNull(enumClass, value);
        return result != null ? result : defaultValue;
    }
}
