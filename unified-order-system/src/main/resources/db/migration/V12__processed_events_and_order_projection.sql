-- Consumidores idempotentes: um evento é aplicado no máximo uma vez por consumidor.
CREATE TABLE IF NOT EXISTS processed_events (
    consumer VARCHAR(100) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (consumer, event_id)
);

-- Read model mantido pelo consumidor RabbitMQ e reconstruível por replay do event store.
CREATE TABLE IF NOT EXISTS order_projection (
    order_id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255),
    customer_name VARCHAR(255),
    total_amount DECIMAL(19, 2),
    status VARCHAR(50) NOT NULL,
    cancellation_reason TEXT,
    last_event_version BIGINT NOT NULL,
    last_event_id VARCHAR(255),
    applied_events INTEGER NOT NULL,
    last_event_at TIMESTAMP
);
