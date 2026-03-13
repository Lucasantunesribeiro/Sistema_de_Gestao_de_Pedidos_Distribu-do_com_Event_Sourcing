CREATE SCHEMA IF NOT EXISTS inventory_service AUTHORIZATION CURRENT_USER;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS inventory_service.event_store (
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
    CONSTRAINT uq_inventory_event_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_inventory_event_aggregate ON inventory_service.event_store(aggregate_id, version);
CREATE INDEX IF NOT EXISTS idx_inventory_event_correlation ON inventory_service.event_store(correlation_id);
CREATE INDEX IF NOT EXISTS idx_inventory_event_occurred ON inventory_service.event_store(occurred_at);

CREATE TABLE IF NOT EXISTS inventory_service.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_inventory_outbox_status ON inventory_service.outbox_events(status, created_at);

CREATE TABLE IF NOT EXISTS inventory_service.processed_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id VARCHAR(255) NOT NULL,
    event_id UUID,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_message UNIQUE (message_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_processed_event ON inventory_service.processed_messages(event_id);

-- Core inventory table
CREATE TABLE IF NOT EXISTS inventory_service.inventory_items (
    product_id VARCHAR(255) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    available_stock INTEGER NOT NULL,
    reserved_stock INTEGER NOT NULL DEFAULT 0,
    price DOUBLE PRECISION NOT NULL,
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_product_name ON inventory_service.inventory_items(product_name);
