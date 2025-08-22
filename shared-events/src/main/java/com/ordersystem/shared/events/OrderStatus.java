package com.ordersystem.shared.events;

public enum OrderStatus {
    PENDING("PENDING", "Pedido criado, aguardando processamento"),
    INVENTORY_RESERVED("INVENTORY_RESERVED", "Estoque reservado com sucesso"),
    INVENTORY_RESERVATION_FAILED("INVENTORY_RESERVATION_FAILED", "Falha na reserva do estoque"),
    PAYMENT_PROCESSING("PAYMENT_PROCESSING", "Processando pagamento"),
    PAYMENT_APPROVED("PAYMENT_APPROVED", "Pagamento aprovado"),
    PAYMENT_FAILED("PAYMENT_FAILED", "Falha no pagamento"),
    CONFIRMED("CONFIRMED", "Pedido confirmado"),
    CANCELLED("CANCELLED", "Pedido cancelado"),
    COMPLETED("COMPLETED", "Pedido concluído"),
    FAILED("FAILED", "Pedido falhou");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status code: " + code);
    }
}