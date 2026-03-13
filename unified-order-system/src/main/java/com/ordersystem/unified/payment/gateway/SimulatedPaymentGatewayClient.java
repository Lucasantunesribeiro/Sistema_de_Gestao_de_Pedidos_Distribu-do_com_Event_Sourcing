package com.ordersystem.unified.payment.gateway;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "payment.gateway", name = "enabled", havingValue = "false", matchIfMissing = true)
public class SimulatedPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentGatewayResponse charge(PaymentGatewayRequest request) {
        PaymentGatewayResponse response = new PaymentGatewayResponse();
        response.setProviderPaymentId("sim-" + shortId());

        if ("BOLETO".equalsIgnoreCase(request.getPaymentMethod())) {
            response.setApproved(false);
            response.setStatus("PENDING");
            response.setTransactionId("BOL-" + shortId());
            response.setMessage("Boleto generated successfully. Pay by the due date.");
            return response;
        }

        String prefix = "PIX".equalsIgnoreCase(request.getPaymentMethod()) ? "PIX-" : "CC-";
        response.setApproved(true);
        response.setStatus("COMPLETED");
        response.setTransactionId(prefix + shortId());
        response.setMessage("Payment processed successfully");
        return response;
    }

    @Override
    public PaymentGatewayResponse refund(PaymentGatewayRefundRequest request) {
        PaymentGatewayResponse response = new PaymentGatewayResponse();
        response.setApproved(true);
        response.setStatus("REFUNDED");
        response.setProviderPaymentId(request.getPaymentId());
        response.setTransactionId("REF-" + shortId());
        response.setMessage("Refund processed successfully");
        return response;
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
