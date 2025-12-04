package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Transaction {
    private final Long id;
    private final TransactionType type;
    private final String description;
    private final String businessRefId;
    private final TransactionStatus status;
    private final Long reversalOfTransactionId;
    private final LocalDateTime createdAt;

    public Transaction(Long id, TransactionType type, String description, String businessRefId,
            TransactionStatus status, Long reversalOfTransactionId, LocalDateTime createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction Type cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Transaction Status cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        this.id = id;
        this.type = type;
        this.description = description;
        this.businessRefId = businessRefId;
        this.status = status;
        this.reversalOfTransactionId = reversalOfTransactionId;
        this.createdAt = createdAt;
    }
}
