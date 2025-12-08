package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.time.Instant;

@Getter
public class Transaction {
    private final Long id;
    private final TransactionType type;
    private final String description;
    private final String businessRefId;
    private final TransactionStatus status;
    private final Long reversalOfTransactionId;
    private final Instant createdAt;

    public Transaction(Long id, TransactionType type, String description, String businessRefId,
            TransactionStatus status, Long reversalOfTransactionId, Instant createdAt) {

        if (type == null) {
            throw new IllegalArgumentException("Transaction Type cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Transaction Status cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        if (status == TransactionStatus.REVERSED && reversalOfTransactionId != null) {
            throw new IllegalArgumentException("A REVERSED transaction cannot be a reversal of another transaction");
        }
        if (reversalOfTransactionId != null && status != TransactionStatus.POSTED) {
            throw new IllegalArgumentException("Reversal transaction must be in POSTED state");
        }
        this.id = id;
        this.type = type;
        this.description = description;
        this.businessRefId = businessRefId;
        this.status = status;
        this.reversalOfTransactionId = reversalOfTransactionId;
        this.createdAt = createdAt;
    }

    public static Transaction createDeposit(String description, String businessRefId, Instant now) {
        return new Transaction(
                null,
                TransactionType.DEPOSIT,
                description,
                businessRefId,
                TransactionStatus.POSTED,
                null,
                now);
    }

    public Transaction toReversed() {
        if (this.status != TransactionStatus.POSTED) {
            throw new IllegalStateException("Only POSTED transactions can be reversed");
        }
        return new Transaction(
                this.id,
                this.type,
                this.description,
                this.businessRefId,
                TransactionStatus.REVERSED,
                this.reversalOfTransactionId,
                this.createdAt);
    }
}
