package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.time.Instant;

@Getter
public class Transaction {
    // 1. 상수
    private static final String REVERSAL_BUSINESS_REF_PREFIX = "reversal-";

    // 2. 인스턴스 필드
    private final Long id;
    private final TransactionType type;
    private final String description;
    private final String businessRefId;
    private final TransactionStatus status;
    private final Long reversalOfTransactionId;
    private final Instant createdAt;

    // 3. 생성자
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

    // 4. 정적 팩토리 메서드
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

    // 5. 공개 메서드 (상태 전환)
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

    public Transaction markAsUnknown() {
        if (this.status != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                    "Only PENDING transactions can be marked as UNKNOWN. Current status: " + this.status);
        }
        return new Transaction(
                this.id,
                this.type,
                this.description,
                this.businessRefId,
                TransactionStatus.UNKNOWN,
                this.reversalOfTransactionId,
                this.createdAt);
    }

    public Transaction resolveUnknown(TransactionStatus targetStatus) {
        if (this.status != TransactionStatus.UNKNOWN) {
            throw new InvalidTransactionStateException(
                    "Only UNKNOWN transactions can be resolved. Current status: " + this.status);
        }
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new InvalidTransactionStateException(
                    "Cannot transition from UNKNOWN to " + targetStatus);
        }
        return new Transaction(
                this.id,
                this.type,
                this.description,
                this.businessRefId,
                targetStatus,
                this.reversalOfTransactionId,
                this.createdAt);
    }

    // 6. 공개 메서드 (검증)
    public void validateCanBeReversed() {
        if (this.status == TransactionStatus.REVERSED) {
            throw new InvalidTransactionStateException(
                    "Transaction is already reversed: " + this.id);
        }
        if (this.status == TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                    "Cannot reverse a PENDING transaction: " + this.id);
        }
        if (this.status == TransactionStatus.UNKNOWN) {
            throw new InvalidTransactionStateException(
                    "Cannot reverse an UNKNOWN transaction. Resolve it first: " + this.id);
        }
        if (!this.status.canBeReversed()) {
            throw new InvalidTransactionStateException(
                    "Transaction cannot be reversed. Status: " + this.status);
        }
    }
}
