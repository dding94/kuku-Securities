package com.securities.kuku.ledger.adapter.out.persistence.entity;

import com.securities.kuku.ledger.domain.JournalEntry;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private JournalEntry.EntryType entryType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public JournalEntryJpaEntity(Long id, Long transactionId, Long accountId,
            BigDecimal amount, JournalEntry.EntryType entryType, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.entryType = entryType;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public JournalEntry toDomain() {
        return new JournalEntry(id, transactionId, accountId, amount, entryType, createdAt);
    }

    public static JournalEntryJpaEntity fromDomain(JournalEntry journalEntry) {
        return new JournalEntryJpaEntity(
                journalEntry.getId(),
                journalEntry.getTransactionId(),
                journalEntry.getAccountId(),
                journalEntry.getAmount(),
                journalEntry.getEntryType(),
                journalEntry.getCreatedAt());
    }
}
