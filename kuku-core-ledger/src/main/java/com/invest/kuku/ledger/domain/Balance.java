package com.invest.kuku.ledger.domain;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class Balance {
    private final Long accountId;
    private final BigDecimal amount;
    private final Long version;
    private final Long lastTransactionId;
    private final LocalDateTime updatedAt;

    public Balance(Long accountId, BigDecimal amount, Long version, Long lastTransactionId, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.amount = amount;
        this.version = version;
        this.lastTransactionId = lastTransactionId;
        this.updatedAt = updatedAt;
    }
}
