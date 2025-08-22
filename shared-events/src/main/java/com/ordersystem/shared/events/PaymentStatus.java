package com.ordersystem.shared.events;

public enum PaymentStatus {
    PENDING("PENDING", "Pagamento pendente"),
    PROCESSING("PROCESSING", "Processando pagamento"),
    APPROVED("APPROVED", "Pagamento aprovado"),
    DECLINED("DECLINED", "Pagamento recusado"),
    FAILED("FAILED", "Falha no pagamento"),
    CANCELLED("CANCELLED", "Pagamento cancelado"),
    REFUNDED("REFUNDED", "Pagamento estornado");

    private final String code;
    private final String description;

    PaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status code: " + code);
    }
}