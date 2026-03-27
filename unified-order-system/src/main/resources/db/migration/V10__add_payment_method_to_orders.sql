ALTER TABLE system_orders
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
