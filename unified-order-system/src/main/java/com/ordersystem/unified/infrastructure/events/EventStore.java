package com.ordersystem.unified.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.domain.events.BaseEvent;
import com.ordersystem.unified.shared.exceptions.ConcurrencyException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event store sobre a tabela {@code domain_events} (que também serve de outbox).
 *
 * <p>Gravação com concorrência otimista: o chamador informa a versão que leu
 * ({@code expectedVersion}). Se o stream já avançou, o append falha com
 * {@link ConcurrencyException} (HTTP 409) em vez de sobrescrever. A restrição única
 * {@code (aggregate_type, aggregate_id, aggregate_version)} cobre a corrida entre a checagem
 * e o insert.
 *
 * <p>Sempre participa da transação do chamador ({@code REQUIRED}): estado e eventos são
 * confirmados ou desfeitos juntos, que é o que o padrão outbox exige.
 */
@Component
public class EventStore {

    private static final Logger logger = LoggerFactory.getLogger(EventStore.class);

    private final DomainEventRepository repository;
    private final ObjectMapper objectMapper;

    public EventStore(DomainEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Acrescenta eventos ao stream exigindo que a versão atual seja {@code expectedVersion}.
     *
     * @return nova versão do stream
     * @throws ConcurrencyException se outro writer gravou antes
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public long append(String aggregateType, String aggregateId, long expectedVersion,
                       List<? extends BaseEvent> events) {
        if (events.isEmpty()) {
            return expectedVersion;
        }
        long currentVersion = repository.findCurrentVersion(aggregateType, aggregateId);
        if (currentVersion != expectedVersion) {
            throw conflict(aggregateType, aggregateId, expectedVersion, currentVersion, null);
        }
        return insert(aggregateType, aggregateId, expectedVersion, events);
    }

    /**
     * Acrescenta eventos no fim do stream sem versão esperada (streams que não têm agregado
     * reconstruído, como Payment e Inventory). A ordem continua garantida pela restrição única.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public long appendToStream(String aggregateType, String aggregateId, List<? extends BaseEvent> events) {
        long currentVersion = repository.findCurrentVersion(aggregateType, aggregateId);
        return insert(aggregateType, aggregateId, currentVersion, events);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEntity> readStream(String aggregateType, String aggregateId) {
        return repository.findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(aggregateType, aggregateId);
    }

    private long insert(String aggregateType, String aggregateId, long fromVersion,
                        List<? extends BaseEvent> events) {
        long version = fromVersion;
        for (BaseEvent event : events) {
            version++;
            repository.save(DomainEventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .aggregateVersion(version)
                .eventType(event.getClass().getSimpleName())
                .eventData(serialize(event))
                .correlationId(event.getCorrelationId())
                .build());
        }
        try {
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw conflict(aggregateType, aggregateId, fromVersion, -1, exception);
        }
        logger.debug("Appended {} event(s) to {}:{} (v{} -> v{})",
            events.size(), aggregateType, aggregateId, fromVersion, version);
        return version;
    }

    private String serialize(BaseEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Event serialization failed: " + event.getClass().getSimpleName(),
                exception);
        }
    }

    private ConcurrencyException conflict(String aggregateType, String aggregateId, long expected, long actual,
                                          Throwable cause) {
        String operation = actual >= 0
            ? String.format("append (expected version %d, actual %d)", expected, actual)
            : String.format("append (expected version %d, stream changed concurrently)", expected);
        logger.warn("Optimistic concurrency conflict on {}:{} - {}", aggregateType, aggregateId, operation);
        String resource = aggregateType + ":" + aggregateId;
        return cause == null
            ? new ConcurrencyException(resource, operation)
            : new ConcurrencyException(resource, operation, cause);
    }
}
