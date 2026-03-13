CREATE SCHEMA IF NOT EXISTS payment_service AUTHORIZATION CURRENT_USER;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS payment_service.event_store (
    event_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    metadata JSONB,
    schema_version VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    CONSTRAINT uq_payment_event_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_payment_event_aggregate ON payment_service.event_store(aggregate_id, version);
CREATE INDEX IF NOT EXISTS idx_payment_event_correlation ON payment_service.event_store(correlation_id);
CREATE INDEX IF NOT EXISTS idx_payment_event_occurred ON payment_service.event_store(occurred_at);

CREATE TABLE IF NOT EXISTS payment_service.outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_payment_outbox_status ON payment_service.outbox_events(status, created_at);

CREATE TABLE IF NOT EXISTS payment_service.processed_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id VARCHAR(255) NOT NULL,
    event_id UUID,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_message UNIQUE (message_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_processed_event ON payment_service.processed_messages(event_id);

-- Core payments table
CREATE TABLE IF NOT EXISTS payment_service.payments (
    payment_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    gateway_transaction_id VARCHAR(100),
    failure_reason VARCHAR(500),
    error_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    correlation_id VARCHAR(36)
);

CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payment_service.payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment_service.payments(status);
