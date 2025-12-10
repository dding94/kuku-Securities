package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.time.Instant;

@Getter
public class Transaction {
    private static final String REVERSAL_BUSINESS_REF_PREFIX = "reversal-";

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

    public static Transaction createWithdraw(String description, String businessRefId, Instant now) {
        return new Transaction(
                null,
                TransactionType.WITHDRAWAL,
                description,
                businessRefId,
                TransactionStatus.POSTED,
                null,
                now);
    }

    public Transaction toReversed() {
        validateCanBeReversed();
        return new Transaction(
                this.id,
                this.type,
                this.description,
                this.businessRefId,
                TransactionStatus.REVERSED,
                this.reversalOfTransactionId,
                this.createdAt);
    }

    public void validateCanBeReversed() {
        if (this.status == TransactionStatus.REVERSED) {
            throw new InvalidTransactionStateException(
                    "Transaction is already reversed: " + this.id);
        }
        if (this.status == TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                    "Cannot reverse a PENDING transaction: " + this.id);
        }
        if (!this.status.canBeReversed()) {
            throw new InvalidTransactionStateException(
                    "Transaction cannot be reversed. Status: " + this.status);
        }
    }

    public static Transaction createReversal(Long originalTransactionId, String reason, Instant now) {
        return new Transaction(
                null,
                TransactionType.REVERSAL,
                reason,
                REVERSAL_BUSINESS_REF_PREFIX + originalTransactionId,
                TransactionStatus.POSTED,
                originalTransactionId,
                now);
    }
}
