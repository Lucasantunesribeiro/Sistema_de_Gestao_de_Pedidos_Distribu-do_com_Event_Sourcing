package com.ordersystem.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
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
