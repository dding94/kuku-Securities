package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Transaction {
    private final Long id;
    /**
     * 트랜잭션의 유형 (예: DEPOSIT, WITHDRAW).
     * <p>
     * 역분개(Reversal) 트랜잭션의 경우, 원본 트랜잭션의 타입을 그대로 따릅니다.
     * 예를 들어, 입금(DEPOSIT)을 취소하는 역분개 트랜잭션의 타입도 DEPOSIT입니다.
     * 실질적인 취소 효과는 JournalEntry의 차변/대변이 반대로 기록됨으로써 발생합니다.
     */
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
