package com.ordersystem.shared.events;

public enum InventoryStatus {
    AVAILABLE("AVAILABLE", "Produto disponível em estoque"),
    RESERVED("RESERVED", "Produto reservado"),
    CONFIRMED("CONFIRMED", "Reserva confirmada"),
    RELEASED("RELEASED", "Reserva liberada"),
    OUT_OF_STOCK("OUT_OF_STOCK", "Produto fora de estoque"),
    LOW_STOCK("LOW_STOCK", "Estoque baixo");

    private final String code;
    private final String description;

    InventoryStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static InventoryStatus fromCode(String code) {
        for (InventoryStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown inventory status code: " + code);
    }
}