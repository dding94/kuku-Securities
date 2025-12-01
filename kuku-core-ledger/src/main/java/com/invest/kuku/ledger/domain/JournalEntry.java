package com.invest.kuku.ledger.domain;

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

    public JournalEntry(Long id, Long transactionId, Long accountId, BigDecimal amount, EntryType entryType) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.entryType = entryType;
        this.createdAt = LocalDateTime.now();
    }
}
