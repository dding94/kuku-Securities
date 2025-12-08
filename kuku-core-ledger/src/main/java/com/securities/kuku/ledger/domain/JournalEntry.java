package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class JournalEntry {
    private final Long id;
    private final Long transactionId;
    private final Long accountId;
    private final BigDecimal amount;
    private final EntryType entryType;
    private final LocalDateTime createdAt;

    public enum EntryType {
        DEBIT, CREDIT
    }

    public JournalEntry(Long id, Long transactionId, Long accountId, BigDecimal amount, EntryType entryType,
            LocalDateTime createdAt) {

        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("EntryType cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.entryType = entryType;
        this.createdAt = createdAt;
    }

    public static JournalEntry createCredit(Long transactionId, Long accountId, BigDecimal amount, LocalDateTime now) {
        return new JournalEntry(
                null,
                transactionId,
                accountId,
                amount,
                EntryType.CREDIT,
                now);
    }
}
