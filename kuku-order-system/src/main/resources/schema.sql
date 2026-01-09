-- Order System Schema

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 8) NOT NULL,
    side VARCHAR(10) NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    price DECIMAL(18, 8),
    status VARCHAR(20) NOT NULL,
    rejected_reason VARCHAR(50),
    business_ref_id VARCHAR(100) UNIQUE,
    executed_price DECIMAL(18, 8),
    executed_quantity DECIMAL(18, 8),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    
    INDEX idx_orders_account_id (account_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at)
);
