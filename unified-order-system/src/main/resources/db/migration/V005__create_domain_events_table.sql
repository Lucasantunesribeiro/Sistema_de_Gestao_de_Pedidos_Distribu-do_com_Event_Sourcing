-- Domain Events table for Event Sourcing
-- Stores all domain events for audit trail and event replay
CREATE TABLE IF NOT EXISTS domain_events (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    correlation_id VARCHAR(255),
    user_id VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for performance optimization
CREATE INDEX idx_domain_events_aggregate_id ON domain_events(aggregate_id);
CREATE INDEX idx_domain_events_event_type ON domain_events(event_type);
CREATE INDEX idx_domain_events_correlation_id ON domain_events(correlation_id);
CREATE INDEX idx_domain_events_created_at ON domain_events(created_at);
CREATE INDEX idx_domain_events_processed ON domain_events(processed);
CREATE INDEX idx_domain_events_aggregate_type ON domain_events(aggregate_type);

-- Composite index for common query patterns
CREATE INDEX idx_domain_events_aggregate_type_created ON domain_events(aggregate_type, created_at DESC);
CREATE INDEX idx_domain_events_correlation_created ON domain_events(correlation_id, created_at ASC);

-- Comment
COMMENT ON TABLE domain_events IS 'Event sourcing store - all domain events for audit and replay';
COMMENT ON COLUMN domain_events.aggregate_id IS 'ID of the aggregate root (e.g., order ID, payment ID)';
COMMENT ON COLUMN domain_events.aggregate_type IS 'Type of aggregate (Order, Payment, Inventory)';
COMMENT ON COLUMN domain_events.event_type IS 'Type of event (OrderCreated, PaymentProcessed, etc.)';
COMMENT ON COLUMN domain_events.event_data IS 'JSON payload of the event';
COMMENT ON COLUMN domain_events.correlation_id IS 'Correlation ID for distributed tracing';
COMMENT ON COLUMN domain_events.processed IS 'Whether event has been processed by consumers';
