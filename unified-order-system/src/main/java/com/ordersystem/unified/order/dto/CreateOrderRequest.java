package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ordersystem.unified.shared.validation.ValidationConstants;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a new order
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderRequest {

    @Size(max = ValidationConstants.MAX_ID_LENGTH, message = ValidationConstants.MSG_ID_TOO_LONG)
    private String customerId;

    @NotBlank(message = "Customer name is required")
    @Size(min = 1, max = ValidationConstants.MAX_NAME_LENGTH, message = "Customer name must be between 1 and " + ValidationConstants.MAX_NAME_LENGTH + " characters")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be valid")
    @Size(max = ValidationConstants.MAX_EMAIL_LENGTH, message = ValidationConstants.MSG_EMAIL_TOO_LONG)
    private String customerEmail;

    private PaymentMethod paymentMethod;

    @NotNull(message = "Order items are required")
    @Size(min = 1, max = ValidationConstants.MAX_ORDER_ITEMS, message = ValidationConstants.MSG_TOO_MANY_ITEMS)
    @Valid
    private List<OrderItemRequest> items;

    private BigDecimal totalAmount;

    private List<String> productIds;

    private String correlationId;

    // Default constructor
    public CreateOrderRequest() {}

    // Constructor with all fields
    public CreateOrderRequest(String customerId, String customerName, String customerEmail, 
                            PaymentMethod paymentMethod, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }

    // Constructor for test compatibility (customerName, customerEmail, items)
    public CreateOrderRequest(String customerName, String customerEmail, List<OrderItemRequest> items) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.items = items;
    }

    // Constructor for test compatibility (customerName, customerEmail, null)
    public CreateOrderRequest(String customerName, String customerEmail, List<OrderItemRequest> items, String correlationId) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.items = items;
        this.correlationId = correlationId;
    }

    // Getters and setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", paymentMethod=" + paymentMethod +
                ", items=" + items +
                '}';
    }
}
