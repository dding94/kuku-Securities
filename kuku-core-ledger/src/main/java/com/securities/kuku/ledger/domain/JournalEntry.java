package com.securities.kuku.ledger.domain;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class JournalEntry {
    private final Long id;
    private final Long transactionId;
    private final Long accountId;
    private final BigDecimal amount;
    private final EntryType entryType;
    private final Instant createdAt;

    public enum EntryType {
        DEBIT, CREDIT
    }

    public JournalEntry(Long id, Long transactionId, Long accountId, BigDecimal amount, EntryType entryType,
            Instant createdAt) {

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

    public static JournalEntry createCredit(Long transactionId, Long accountId, BigDecimal amount, Instant now) {
        return new JournalEntry(
                null,
                transactionId,
                accountId,
                amount,
                EntryType.CREDIT,
                now);
    }

    public static JournalEntry createDebit(Long transactionId, Long accountId, BigDecimal amount, Instant now) {
        return new JournalEntry(
                null,
                transactionId,
                accountId,
                amount,
                EntryType.DEBIT,
                now);
    }

    public Balance applyReverseTo(Balance balance, Long transactionId, Instant now) {
        return switch (this.entryType) {
            case CREDIT -> balance.withdraw(amount, transactionId, now);
            case DEBIT -> balance.deposit(amount, transactionId, now);
        };
    }

    public JournalEntry createOpposite(Long transactionId, Instant now) {
        return switch (this.entryType) {
            case CREDIT -> createDebit(transactionId, this.accountId, this.amount, now);
            case DEBIT -> createCredit(transactionId, this.accountId, this.amount, now);
        };
    }
}
