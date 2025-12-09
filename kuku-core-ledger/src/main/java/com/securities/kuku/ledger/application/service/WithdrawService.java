package com.securities.kuku.ledger.application.service;

import com.securities.kuku.ledger.application.port.in.WithdrawUseCase;
import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;
import com.securities.kuku.ledger.application.port.out.LoadAccountPort;
import com.securities.kuku.ledger.application.port.out.LoadBalancePort;
import com.securities.kuku.ledger.application.port.out.LoadTransactionPort;
import com.securities.kuku.ledger.application.port.out.SaveJournalEntryPort;
import com.securities.kuku.ledger.application.port.out.SaveTransactionPort;
import com.securities.kuku.ledger.application.port.out.UpdateBalancePort;
import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.InsufficientBalanceException;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService implements WithdrawUseCase {

    private final Clock clock;
    private final LoadAccountPort loadAccountPort;
    private final LoadBalancePort loadBalancePort;
    private final SaveTransactionPort saveTransactionPort;
    private final SaveJournalEntryPort saveJournalEntryPort;
    private final UpdateBalancePort updateBalancePort;
    private final LoadTransactionPort loadTransactionPort;

    @Override
    @Transactional
    public void withdraw(WithdrawCommand command) {
        // 1. Idempotency Check
        if (isDuplicateTransaction(command.businessRefId())) {
            log.warn("Duplicate transaction detected. businessRefId={}", command.businessRefId());
            return;
        }

        // 2. Load Aggregates
        Account account = loadAccountPort.loadAccount(command.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + command.accountId()));

        Balance balance = loadBalancePort.loadBalance(command.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Balance not found: " + command.accountId()));

        // Capture semantic time for this operation
        Instant now = clock.instant();

        // 3. Validate sufficient balance (fail-fast before any writes)
        validateSufficientBalance(balance, command.amount());

        // 4. Create & Save Transaction
        Transaction transaction = Transaction.createWithdraw(command.description(), command.businessRefId(), now);
        Transaction savedTransaction = saveTransactionPort.saveTransaction(transaction);

        // 5. Create & Save Journal Entry
        JournalEntry journalEntry = JournalEntry.createDebit(
                savedTransaction.getId(),
                account.getId(),
                command.amount(),
                now);
        saveJournalEntryPort.saveJournalEntry(journalEntry);

        // 6. Update Balance
        Balance newBalance = balance.withdraw(command.amount(), savedTransaction.getId(), now);
        updateBalancePort.updateBalance(newBalance);
    }

    private void validateSufficientBalance(Balance balance, BigDecimal amount) {
        if (amount.compareTo(balance.getAvailableAmount()) > 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + balance.getAvailableAmount() + ", Requested: " + amount);
        }
    }

    private boolean isDuplicateTransaction(String businessRefId) {
        return loadTransactionPort.loadTransaction(businessRefId).isPresent();
    }
}
