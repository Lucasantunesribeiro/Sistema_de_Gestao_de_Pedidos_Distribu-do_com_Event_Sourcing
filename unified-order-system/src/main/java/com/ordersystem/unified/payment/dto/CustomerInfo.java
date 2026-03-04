package com.ordersystem.unified.payment.dto;

import com.ordersystem.unified.shared.validation.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for customer information in payment requests
 */
public class CustomerInfo {

    @Size(max = ValidationConstants.MAX_ID_LENGTH, message = ValidationConstants.MSG_ID_TOO_LONG)
    private String customerId;

    @NotBlank(message = "Customer name is required")
    @Size(max = ValidationConstants.MAX_NAME_LENGTH, message = ValidationConstants.MSG_NAME_TOO_LONG)
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be valid")
    @Size(max = ValidationConstants.MAX_EMAIL_LENGTH, message = ValidationConstants.MSG_EMAIL_TOO_LONG)
    private String customerEmail;

    @Size(max = ValidationConstants.MAX_PHONE_LENGTH, message = "Phone number exceeds maximum length")
    private String customerPhone;

    @Size(max = ValidationConstants.MAX_ADDRESS_LENGTH, message = ValidationConstants.MSG_ADDRESS_TOO_LONG)
    private String billingAddress;

    // Default constructor
    public CustomerInfo() {}

    // Constructor with required fields
    public CustomerInfo(String customerId, String customerName, String customerEmail) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }

    // Constructor with all fields
    public CustomerInfo(String customerId, String customerName, String customerEmail, 
                       String customerPhone, String billingAddress) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.billingAddress = billingAddress;
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

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    // Additional methods for test compatibility
    public void setName(String customerName) {
        this.customerName = customerName;
    }

    public void setEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    @Override
    public String toString() {
        return "CustomerInfo{" +
                "customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerPhone='" + customerPhone + '\'' +
                ", billingAddress='" + billingAddress + '\'' +
                '}';
    }
}