package com.securities.kuku.ledger.application.service;

import com.securities.kuku.ledger.application.port.in.ReversalUseCase;
import com.securities.kuku.ledger.application.port.in.command.ReversalCommand;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReversalService implements ReversalUseCase {

    private final Clock clock;
    private final TransactionPort transactionPort;
    private final JournalEntryPort journalEntryPort;
    private final BalancePort balancePort;

    @Override
    @Transactional
    public void reverse(ReversalCommand command) {
        // 1. Load original transaction
        Transaction originalTransaction = transactionPort
                .findById(command.originalTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found: " + command.originalTransactionId()));

        // 2. Validate transaction state (Delegated to Domain Entity)
        originalTransaction.validateCanBeReversed();

        // 3. Load original journal entries
        List<JournalEntry> originalEntries = journalEntryPort
                .findByTransactionId(command.originalTransactionId());

        // 4. Validate journal entries exist (data integrity check)
        validateJournalEntriesExist(originalEntries, command.originalTransactionId());

        // 5. Batch load all required balances (N+1 SELECT 방지)
        Set<Long> accountIds = originalEntries.stream()
                .map(JournalEntry::getAccountId)
                .collect(Collectors.toSet());
        Map<Long, Balance> balanceMap = balancePort.findByAccountIds(accountIds);

        // Capture semantic time
        Instant now = clock.instant();

        // 6. Mark original transaction as REVERSED
        Transaction reversedOriginal = originalTransaction.toReversed();
        transactionPort.update(reversedOriginal);

        // 7. Create & Save reversal transaction
        Transaction reversalTransaction = Transaction.createReversal(
                command.originalTransactionId(),
                command.reason(),
                now);
        Transaction savedReversal = transactionPort.save(reversalTransaction);

        // 8. Create opposite journal entries (batch)
        List<JournalEntry> oppositeEntries = createOppositeEntries(originalEntries, savedReversal.getId(), now);
        journalEntryPort.saveAll(oppositeEntries);

        // 9. Restore balances (batch)
        List<Balance> restoredBalances = restoreBalances(originalEntries, balanceMap, savedReversal.getId(), now);
        balancePort.updateAll(restoredBalances);
    }

    private void validateJournalEntriesExist(List<JournalEntry> entries, Long transactionId) {
        if (entries.isEmpty()) {
            throw new IllegalStateException(
                    "No journal entries found for transaction: " + transactionId
                            + ". Data integrity issue detected.");
        }
    }

    private List<JournalEntry> createOppositeEntries(List<JournalEntry> originals, Long reversalTransactionId,
            Instant now) {
        List<JournalEntry> oppositeEntries = new ArrayList<>();
        for (JournalEntry original : originals) {
            JournalEntry opposite;
            if (original.getEntryType() == JournalEntry.EntryType.CREDIT) {
                opposite = JournalEntry.createDebit(
                        reversalTransactionId,
                        original.getAccountId(),
                        original.getAmount(),
                        now);
            } else {
                opposite = JournalEntry.createCredit(
                        reversalTransactionId,
                        original.getAccountId(),
                        original.getAmount(),
                        now);
            }
            oppositeEntries.add(opposite);
        }
        return oppositeEntries;
    }

    private List<Balance> restoreBalances(List<JournalEntry> originalEntries, Map<Long, Balance> balanceMap,
            Long transactionId, Instant now) {
        List<Balance> restoredBalances = new ArrayList<>();

        for (JournalEntry originalEntry : originalEntries) {
            Long accountId = originalEntry.getAccountId();
            Balance balance = balanceMap.get(accountId);

            if (balance == null) {
                throw new IllegalArgumentException("Balance not found: " + accountId);
            }

            Balance restoredBalance;
            if (originalEntry.getEntryType() == JournalEntry.EntryType.CREDIT) {
                // Original was CREDIT (deposit) -> reverse by withdrawing
                restoredBalance = balance.withdraw(originalEntry.getAmount(), transactionId, now);
            } else {
                // Original was DEBIT (withdraw) -> reverse by depositing
                restoredBalance = balance.deposit(originalEntry.getAmount(), transactionId, now);
            }

            // Update map for subsequent operations on same account
            balanceMap.put(accountId, restoredBalance);
            restoredBalances.add(restoredBalance);
        }

        return restoredBalances;
    }
}
