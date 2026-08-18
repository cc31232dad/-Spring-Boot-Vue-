ALTER TABLE orders
    ADD COLUMN order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER farmer_id,
    ADD INDEX idx_orders_type (order_type);

CREATE TABLE seckill_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    seckill_price DECIMAL(10, 2) NOT NULL,
    total_stock INT NOT NULL,
    limit_per_user INT NOT NULL DEFAULT 1,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_seckill_price CHECK (seckill_price >= 0),
    CONSTRAINT chk_seckill_stock CHECK (total_stock > 0),
    CONSTRAINT chk_seckill_limit CHECK (limit_per_user > 0),
    CONSTRAINT chk_seckill_time CHECK (end_at > start_at),
    INDEX idx_seckill_status_time (status, start_at, end_at),
    INDEX idx_seckill_product (product_id)
);

CREATE TABLE seckill_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_seckill_activity_buyer UNIQUE (activity_id, buyer_id),
    CONSTRAINT uk_seckill_order UNIQUE (order_id),
    CONSTRAINT chk_seckill_order_quantity CHECK (quantity > 0),
    INDEX idx_seckill_orders_buyer (buyer_id),
    INDEX idx_seckill_orders_activity (activity_id)
);
