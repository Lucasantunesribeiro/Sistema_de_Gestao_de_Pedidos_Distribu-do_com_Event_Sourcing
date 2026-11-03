package com.ordersystem.unified.infrastructure.messaging;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executa o efeito de uma mensagem no máximo uma vez por {@code (consumer, eventId)}.
 *
 * <p>Na mesma transação: (1) se o par já está em {@code processed_events}, não faz nada;
 * (2) senão insere o marcador (flush imediato) e (3) aplica o efeito. Se o efeito falhar, o marcador
 * sofre rollback junto e a mensagem pode ser reprocessada. Duas entregas simultâneas da mesma
 * mensagem: a segunda bate na chave primária, faz rollback e, no retry, é descartada como duplicata.
 *
 * <p>O listener só retorna (e o container só faz ack, modo AUTO) depois do commit desta transação.
 */
@Component
public class IdempotentMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(IdempotentMessageProcessor.class);

    private final ProcessedEventRepository repository;

    public IdempotentMessageProcessor(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /**
     * @return {@code true} se o efeito foi aplicado; {@code false} se era duplicata
     */
    @Transactional
    public boolean processOnce(String consumer, String eventId, Runnable effect) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required for idempotent processing");
        }
        if (repository.existsById(new ProcessedEventId(consumer, eventId))) {
            logger.info("Duplicate delivery ignored: consumer={}, eventId={}", consumer, eventId);
            return false;
        }
        repository.saveAndFlush(new ProcessedEvent(consumer, eventId, LocalDateTime.now()));
        effect.run();
        return true;
    }
}
