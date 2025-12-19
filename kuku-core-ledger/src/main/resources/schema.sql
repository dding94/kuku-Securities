CREATE TABLE IF NOT EXISTS `accounts` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `account_number` VARCHAR(50) NOT NULL,
    `currency` VARCHAR(10) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_number` (`account_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- transactions: 트랜잭션 테이블
-- status 컬럼:
--   PENDING  - 트랜잭션이 생성되었으나 아직 확정되지 않음
--   POSTED   - 트랜잭션이 확정되어 잔액에 반영됨
--   REVERSED - 역분개되어 무효화됨
--   UNKNOWN  - 외부 시스템 Timeout 등으로 상태 확인이 필요함
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255),
    `business_ref_id` VARCHAR(100),
    `status` VARCHAR(20) NOT NULL,
    `reversal_of_transaction_id` BIGINT,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_ref_id` (`business_ref_id`),
    KEY `idx_reversal_of_transaction_id` (`reversal_of_transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `journal_entries` (
    `id` BIGINT NOT NULL,
    `transaction_id` BIGINT NOT NULL,
    `account_id` BIGINT NOT NULL,
    `amount` DECIMAL(19, 4) NOT NULL,
    `entry_type` VARCHAR(20) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `balances` (
    `account_id` BIGINT NOT NULL,
    `amount` DECIMAL(19, 4) NOT NULL DEFAULT 0,
    `hold_amount` DECIMAL(19, 4) NOT NULL DEFAULT 0,
    `version` BIGINT NOT NULL DEFAULT 0,
    `last_transaction_id` BIGINT,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
