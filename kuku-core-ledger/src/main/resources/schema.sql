CREATE TABLE IF NOT EXISTS `accounts` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `account_number` VARCHAR(50) NOT NULL,
    `currency` VARCHAR(10) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_number` (`account_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `transactions` (
    `id` BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255),
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`)
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
    `version` BIGINT NOT NULL DEFAULT 0,
    `last_transaction_id` BIGINT,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
