package com.ordersystem.unified.payment.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "payment.gateway", name = "enabled", havingValue = "true")
public class HttpPaymentGatewayClient implements PaymentGatewayClient {

    private final PaymentGatewayProperties properties;
    private final RestTemplate restTemplate;

    public HttpPaymentGatewayClient(RestTemplateBuilder restTemplateBuilder,
                                    PaymentGatewayProperties properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("payment.gateway.base-url must be configured when the gateway is enabled");
        }
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(properties.getConnectTimeout())
            .setReadTimeout(properties.getReadTimeout())
            .build();
    }

    @Override
    public PaymentGatewayResponse charge(PaymentGatewayRequest request) {
        return restTemplate.postForObject(
            properties.getBaseUrl() + "/payments",
            buildRequestEntity(request),
            PaymentGatewayResponse.class
        );
    }

    @Override
    public PaymentGatewayResponse refund(PaymentGatewayRefundRequest request) {
        return restTemplate.postForObject(
            properties.getBaseUrl() + "/refunds",
            buildRequestEntity(request),
            PaymentGatewayResponse.class
        );
    }

    private HttpEntity<Object> buildRequestEntity(Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(properties.getApiKey())) {
            headers.set("X-API-Key", properties.getApiKey());
        }
        return new HttpEntity<>(payload, headers);
    }
}
