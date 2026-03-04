CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255),
    payment_method VARCHAR(50),
    failure_reason TEXT,
    error_code VARCHAR(50),
    processed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
