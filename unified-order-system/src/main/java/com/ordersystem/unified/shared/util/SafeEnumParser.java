package com.ordersystem.unified.shared.util;

public class SafeEnumParser {
    public static <T extends Enum<T>> T parseEnumOrThrow(Class<T> enumType, String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String validValues = java.util.Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid value for " + fieldName + ": " + value + ". Valid values: " + validValues);
        }
    }
}
