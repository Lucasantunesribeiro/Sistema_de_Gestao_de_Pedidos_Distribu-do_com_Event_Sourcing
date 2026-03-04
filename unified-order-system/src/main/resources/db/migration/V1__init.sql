CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
ALTER EXTENSION "uuid-ossp"
SET SCHEMA public;
-- Orders Table
CREATE TABLE system_orders (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(255),
    reservation_id VARCHAR(255),
    payment_id VARCHAR(255),
    transaction_id VARCHAR(255),
    cancellation_reason TEXT
);
CREATE INDEX idx_orders_customer_id ON system_orders(customer_id);
CREATE INDEX idx_orders_status ON system_orders(status);
CREATE INDEX idx_orders_correlation_id ON system_orders(correlation_id);
-- Order Items Table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);
-- Users Table (for Authentication)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    roles VARCHAR(255) NOT NULL,
    -- Comma separated roles
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_users_username ON users(username);