package com.ordersystem.unified.payment;

import com.ordersystem.unified.shared.events.PaymentStatus;

/**
 * Result of a payment processing operation.
 */
public class PaymentResult {
    
    private final boolean success;
    private final PaymentStatus status;
    private final String paymentId;
    private final String message;
    private final String errorCode;

    private PaymentResult(boolean success, PaymentStatus status, String paymentId, String message, String errorCode) {
        this.success = success;
        this.status = status;
        this.paymentId = paymentId;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static PaymentResult success(String paymentId) {
        return new PaymentResult(true, PaymentStatus.COMPLETED, paymentId, "Payment processed successfully", null);
    }

    public static PaymentResult failure(String message, String errorCode) {
        return new PaymentResult(false, PaymentStatus.FAILED, null, message, errorCode);
    }

    public static PaymentResult pending(String paymentId) {
        return new PaymentResult(false, PaymentStatus.PENDING, paymentId, "Payment is being processed", null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public PaymentStatus getStatus() { return status; }
    public String getPaymentId() { return paymentId; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }

    @Override
    public String toString() {
        return String.format("PaymentResult{success=%s, status=%s, paymentId='%s', message='%s'}",
                success, status, paymentId, message);
    }
}