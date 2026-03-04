package com.ordersystem.common.security;

/**
 * Centralized validation constants for input validation.
 * Defines maximum lengths and sizes to prevent memory exhaustion attacks.
 */
public final class ValidationConstants {

    private ValidationConstants() {
        // Utility class
    }

    // String length limits
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_UUID_LENGTH = 36;
    public static final int MAX_NAME_LENGTH = 255;
    public static final int MAX_EMAIL_LENGTH = 320;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_ADDRESS_LENGTH = 500;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_REASON_LENGTH = 1000;
    public static final int MAX_NOTES_LENGTH = 2000;

    // Product/Item specific
    public static final int MAX_PRODUCT_NAME_LENGTH = 200;
    public static final int MAX_SKU_LENGTH = 50;
    public static final int MAX_CATEGORY_LENGTH = 100;

    // Collection size limits
    public static final int MAX_ORDER_ITEMS = 100;
    public static final int MAX_RESERVATION_ITEMS = 100;
    public static final int MAX_BATCH_SIZE = 1000;

    // Numeric limits
    public static final int MAX_QUANTITY = 10000;
    public static final String MAX_PRICE = "999999.99";
    public static final String MIN_PRICE = "0.00";

    // Validation messages
    public static final String MSG_ID_TOO_LONG = "ID exceeds maximum length of " + MAX_ID_LENGTH;
    public static final String MSG_NAME_TOO_LONG = "Name exceeds maximum length of " + MAX_NAME_LENGTH;
    public static final String MSG_EMAIL_TOO_LONG = "Email exceeds maximum length of " + MAX_EMAIL_LENGTH;
    public static final String MSG_ADDRESS_TOO_LONG = "Address exceeds maximum length of " + MAX_ADDRESS_LENGTH;
    public static final String MSG_TOO_MANY_ITEMS = "Too many items (maximum: " + MAX_ORDER_ITEMS + ")";
    public static final String MSG_INVALID_QUANTITY = "Quantity must be between 1 and " + MAX_QUANTITY;
}
