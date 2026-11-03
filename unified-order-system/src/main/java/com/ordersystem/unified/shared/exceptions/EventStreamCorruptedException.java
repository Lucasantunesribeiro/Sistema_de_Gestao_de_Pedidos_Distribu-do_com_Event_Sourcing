package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Lançada quando o stream de eventos de um agregado não pode ser reproduzido
 * (versões fora de ordem/lacunas, evento desconhecido ou transição impossível).
 * Indica dado inconsistente no event store, não erro do cliente.
 */
public class EventStreamCorruptedException extends OrderSystemException {

    public EventStreamCorruptedException(String message) {
        super(message, "EVENT_STREAM_CORRUPTED", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public EventStreamCorruptedException(String message, Throwable cause) {
        super(message, "EVENT_STREAM_CORRUPTED", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
