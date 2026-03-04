-- Add optimistic locking version column to system_orders
ALTER TABLE system_orders ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
