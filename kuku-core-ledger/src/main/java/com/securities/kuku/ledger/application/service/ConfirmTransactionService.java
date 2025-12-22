package com.securities.kuku.ledger.application.service;

import com.securities.kuku.ledger.application.port.in.ConfirmTransactionUseCase;
import com.securities.kuku.ledger.application.port.in.command.ConfirmTransactionCommand;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmTransactionService implements ConfirmTransactionUseCase {

    private final Clock clock;
    private final TransactionPort transactionPort;
    private final BalancePort balancePort;
    private final JournalEntryPort journalEntryPort;

    @Override
    @Transactional
    public void confirm(ConfirmTransactionCommand command) {
        Transaction transaction = transactionPort.findById(command.transactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found: " + command.transactionId()));

        Instant now = clock.instant();

        Transaction confirmedTx = transaction.confirm();
        transactionPort.update(confirmedTx);

        JournalEntry journalEntry = confirmedTx.createJournalEntry(command.accountId(), command.amount(), now);
        journalEntryPort.save(journalEntry);

        Balance balance = balancePort.findByAccountId(command.accountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Balance not found: " + command.accountId()));

        Balance newBalance = confirmedTx.getType().applyTo(balance, command.amount(), confirmedTx.getId(), now);
        balancePort.update(newBalance);
    }
}
