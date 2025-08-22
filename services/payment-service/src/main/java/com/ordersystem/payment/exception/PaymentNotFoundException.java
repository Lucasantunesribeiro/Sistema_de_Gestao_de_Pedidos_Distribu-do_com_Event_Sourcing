package com.ordersystem.payment.exception;

public class PaymentNotFoundException extends RuntimeException {
    
    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super("Pagamento não encontrado: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentNotFoundException(String paymentId, String message) {
        super(message);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}