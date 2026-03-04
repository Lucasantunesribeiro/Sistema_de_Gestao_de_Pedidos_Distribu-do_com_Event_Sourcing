package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for cancelling an order.
 * Contains cancellation reason and optional metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 3, max = 500, message = "Cancellation reason must be between 3 and 500 characters")
    private String reason;

    private String cancelledBy; // User ID who initiated cancellation

    private boolean forceCancel; // Force cancellation even if in terminal state (admin only)

    // Default constructor
    public CancelOrderRequest() {}

    public CancelOrderRequest(String reason) {
        this.reason = reason;
    }

    public CancelOrderRequest(String reason, String cancelledBy) {
        this.reason = reason;
        this.cancelledBy = cancelledBy;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public boolean isForceCancel() {
        return forceCancel;
    }

    public void setForceCancel(boolean forceCancel) {
        this.forceCancel = forceCancel;
    }

    @Override
    public String toString() {
        return String.format(
            "CancelOrderRequest{reason='%s', cancelledBy='%s', forceCancel=%b}",
            reason, cancelledBy, forceCancel
        );
    }
}
