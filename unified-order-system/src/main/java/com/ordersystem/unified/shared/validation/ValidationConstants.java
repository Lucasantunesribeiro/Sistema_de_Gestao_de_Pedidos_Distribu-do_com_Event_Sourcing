package com.ordersystem.unified.shared.validation;

public final class ValidationConstants {
    
    // Lengths
    public static final int MAX_ID_LENGTH = 50;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_ADDRESS_LENGTH = 255;
    
    // Messages
    public static final String MSG_ID_TOO_LONG = "ID exceeds maximum length of " + MAX_ID_LENGTH;
    public static final String MSG_NAME_TOO_LONG = "Name exceeds maximum length of " + MAX_NAME_LENGTH;
    public static final String MSG_EMAIL_TOO_LONG = "Email exceeds maximum length of " + MAX_EMAIL_LENGTH;
    public static final String MSG_PHONE_TOO_LONG = "Phone exceeds maximum length of " + MAX_PHONE_LENGTH;
    public static final String MSG_ADDRESS_TOO_LONG = "Address exceeds maximum length of " + MAX_ADDRESS_LENGTH;

    // Order Limits
    public static final int MAX_ORDER_ITEMS = 50;
    public static final String MSG_TOO_MANY_ITEMS = "Order cannot exceed " + MAX_ORDER_ITEMS + " items";
    
    public static final int MAX_PRODUCT_NAME_LENGTH = 100;
    public static final int MAX_QUANTITY = 100000;
    public static final String MSG_INVALID_QUANTITY = "Quantity cannot exceed " + MAX_QUANTITY;

    public static final int MAX_RESERVATION_ITEMS = 50;  // Same as order items limit

    private ValidationConstants() {
        // Prevent instantiation
    }
}
