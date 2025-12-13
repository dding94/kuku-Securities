package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.JournalEntryJpaEntity;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.domain.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JournalEntryPersistenceAdapter implements JournalEntryPort {

    private final JournalEntryJpaRepository journalEntryJpaRepository;

    @Override
    public void save(JournalEntry journalEntry) {
        JournalEntryJpaEntity entity = JournalEntryJpaEntity.fromDomain(journalEntry);
        journalEntryJpaRepository.save(entity);
    }

    @Override
    public void saveAll(Collection<JournalEntry> journalEntries) {
        List<JournalEntryJpaEntity> entities = journalEntries.stream()
                .map(JournalEntryJpaEntity::fromDomain)
                .toList();
        journalEntryJpaRepository.saveAll(entities);
    }

    @Override
    public List<JournalEntry> findByTransactionId(Long transactionId) {
        return journalEntryJpaRepository.findByTransactionId(transactionId)
                .stream()
                .map(JournalEntryJpaEntity::toDomain)
                .toList();
    }
}
