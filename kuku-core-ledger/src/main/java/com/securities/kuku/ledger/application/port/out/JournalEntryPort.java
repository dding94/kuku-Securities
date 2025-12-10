package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.JournalEntry;
import java.util.Collection;
import java.util.List;

public interface JournalEntryPort {

    void save(JournalEntry journalEntry);

    void saveAll(Collection<JournalEntry> journalEntries);

    List<JournalEntry> findByTransactionId(Long transactionId);
}
