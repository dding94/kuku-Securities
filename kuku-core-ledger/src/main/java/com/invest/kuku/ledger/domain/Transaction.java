package com.invest.kuku.ledger.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Transaction {
    private final Long id;
    private final String type;
    private final String description;
    private final LocalDateTime createdAt;

    public Transaction(Long id, String type, String description) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
