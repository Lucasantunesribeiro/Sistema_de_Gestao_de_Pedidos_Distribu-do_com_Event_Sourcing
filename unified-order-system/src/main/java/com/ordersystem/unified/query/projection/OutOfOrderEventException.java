package com.ordersystem.unified.query.projection;

/**
 * Evento chegou com lacuna de versão (ex.: v3 antes de v2). A mensagem é rejeitada; após o retry
 * vai para a DLQ e a projeção pode ser corrigida com o rebuild por replay.
 */
public class OutOfOrderEventException extends RuntimeException {

    public OutOfOrderEventException(String message) {
        super(message);
    }
}
