-- Create Products Table
CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  sku VARCHAR(255),
  price DECIMAL(10, 2),
  category VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  weight FLOAT8,
  dimensions VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
-- Create Stocks Table
CREATE TABLE IF NOT EXISTS stocks (
  id VARCHAR(255) PRIMARY KEY,
  product_id VARCHAR(255) NOT NULL,
  warehouse_id VARCHAR(255) NOT NULL DEFAULT 'DEFAULT',
  available_quantity INTEGER NOT NULL DEFAULT 0,
  reserved_quantity INTEGER NOT NULL DEFAULT 0,
  total_quantity INTEGER NOT NULL DEFAULT 0,
  minimum_stock INTEGER DEFAULT 0,
  maximum_stock INTEGER,
  reorder_point INTEGER,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_stock_update TIMESTAMP,
  CONSTRAINT fk_stocks_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uq_stocks_product_warehouse UNIQUE (product_id, warehouse_id)
);
-- Create Reservations Table
CREATE TABLE IF NOT EXISTS reservations (
  id VARCHAR(255) PRIMARY KEY,
  order_id VARCHAR(255) NOT NULL,
  status VARCHAR(50) NOT NULL,
  expiry_time TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON reservations(order_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
-- Create Reservation Items Table
CREATE TABLE IF NOT EXISTS reservation_items (
  id VARCHAR(255) PRIMARY KEY,
  reservation_id VARCHAR(255) NOT NULL,
  product_id VARCHAR(255) NOT NULL,
  stock_id VARCHAR(255),
  requested_quantity INTEGER NOT NULL,
  reserved_quantity INTEGER NOT NULL,
  confirmed_quantity INTEGER DEFAULT 0,
  released_quantity INTEGER DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reservation_items_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
  CONSTRAINT fk_reservation_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_reservation_items_stock FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
-- Create Payments Table (from V2 which was ignored)
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