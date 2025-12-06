package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.JournalEntry;

public interface SaveJournalEntryPort {
    void saveJournalEntry(JournalEntry journalEntry);
}
