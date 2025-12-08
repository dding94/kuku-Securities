package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.time.Instant;

@Getter
public class Account {
    private final Long id;
    private final Long userId;
    private final String accountNumber;
    private final String currency;

    private final AccountType type;
    private final Instant createdAt;

    public Account(Long id, Long userId, String accountNumber, String currency, AccountType type,
            Instant createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("AccountType cannot be null");
        }
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.type = type;
        this.createdAt = createdAt;
    }
}
