package com.invest.kuku.ledger.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Transaction {
    private final Long id;
    private final String type;
    private final String description;
    private final LocalDateTime createdAt;

    public Transaction(Long id, String type, String description, LocalDateTime createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Transaction Type cannot be null or empty");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        this.id = id;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }
}
