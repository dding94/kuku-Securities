package com.invest.kuku.ledger.domain;

import lombok.Getter;

@Getter
public class Account {
    private final Long id;
    private final Long userId;
    private final String accountNumber;
    private final String currency;

    public Account(Long id, Long userId, String accountNumber, String currency) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.currency = currency;
    }
}
