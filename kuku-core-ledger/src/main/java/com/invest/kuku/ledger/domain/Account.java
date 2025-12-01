package com.invest.kuku.ledger.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Account {
    private final Long id;
    private final Long userId;
    private final String accountNumber;
    private final String currency;

    private final LocalDateTime createdAt;

    public Account(Long id, Long userId, String accountNumber, String currency, LocalDateTime createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.createdAt = createdAt;
    }
}
