package com.ordersystem.unified.payment.gateway;

public interface PaymentGatewayClient {

    PaymentGatewayResponse charge(PaymentGatewayRequest request);

    PaymentGatewayResponse refund(PaymentGatewayRefundRequest request);
}
