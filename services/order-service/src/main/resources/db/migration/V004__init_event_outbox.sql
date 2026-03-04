-- schema and tables for order service event sourcing/outbox
CREATE SCHEMA IF NOT EXISTS order_service AUTHORIZATION CURRENT_USER;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS order_service.event_store (
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
    CONSTRAINT uq_order_event_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_order_event_aggregate ON order_service.event_store(aggregate_id, version);
CREATE INDEX IF NOT EXISTS idx_order_event_correlation ON order_service.event_store(correlation_id);
CREATE INDEX IF NOT EXISTS idx_order_event_occurred ON order_service.event_store(occurred_at);

CREATE TABLE IF NOT EXISTS order_service.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_order_outbox_status ON order_service.outbox_events(status, created_at);

CREATE TABLE IF NOT EXISTS order_service.processed_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id VARCHAR(255) NOT NULL,
    event_id UUID,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_order_message UNIQUE (message_id)
);

CREATE INDEX IF NOT EXISTS idx_order_processed_event ON order_service.processed_messages(event_id);
