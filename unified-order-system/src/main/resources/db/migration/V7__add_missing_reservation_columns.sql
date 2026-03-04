-- Add missing columns to reservations table
-- The Reservation entity requires these columns that were not in V6
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS released_at TIMESTAMP;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS warehouse_id VARCHAR(255);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_reservations_warehouse_id ON reservations(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_reservations_correlation_id ON reservations(correlation_id);

-- Recreate order_items table with correct schema for OrderItemEntity
-- V1 created order_items as an event store, but the entity maps to order line items
ALTER TABLE order_items RENAME TO order_events_legacy;

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    total_price DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES system_orders(id)
);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
