package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class Balance {
    private final Long accountId;
    private final BigDecimal amount;
    private final BigDecimal holdAmount;
    private final Long version;
    private final Long lastTransactionId;
    private final LocalDateTime updatedAt;

    public Balance(Long accountId, BigDecimal amount, BigDecimal holdAmount, Long version, Long lastTransactionId,
            LocalDateTime updatedAt) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (holdAmount == null || holdAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("HoldAmount cannot be null or negative");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("UpdatedAt cannot be null");
        }
        this.accountId = accountId;
        this.amount = amount;
        this.holdAmount = holdAmount;
        this.version = version;
        this.lastTransactionId = lastTransactionId;
        this.updatedAt = updatedAt;
    }

    public BigDecimal getAvailableAmount() {
        return amount.subtract(holdAmount);
    }
}
