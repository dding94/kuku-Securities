package com.securities.kuku.ledger.application.service;

import com.securities.kuku.ledger.application.port.in.DepositUseCase;
import com.securities.kuku.ledger.application.port.in.command.DepositCommand;
import com.securities.kuku.ledger.application.port.out.AccountPort;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionType;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService implements DepositUseCase {

  private final Clock clock;
  private final AccountPort accountPort;
  private final BalancePort balancePort;
  private final TransactionPort transactionPort;
  private final JournalEntryPort journalEntryPort;
  private final OutboxEventRecorder outboxEventRecorder;

  @Override
  @Retryable(
      retryFor = ObjectOptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000))
  @Transactional
  public void deposit(DepositCommand command) {
    // 1. Idempotency Check
    if (isDuplicateTransaction(command.businessRefId())) {
      log.warn("Duplicate transaction detected. businessRefId={}", command.businessRefId());
      return;
    }

    // 2. Load Aggregates
    Account account =
        accountPort
            .findById(command.accountId())
            .orElseThrow(
                () -> new IllegalArgumentException("Account not found: " + command.accountId()));

    Balance balance =
        balancePort
            .findByAccountId(command.accountId())
            .orElseThrow(
                () -> new IllegalArgumentException("Balance not found: " + command.accountId()));

    // Capture semantic time for this operation
    Instant now = clock.instant();

    // 3. Create & Save Transaction
    Transaction transaction =
        Transaction.createDeposit(command.description(), command.businessRefId(), now);
    Transaction savedTransaction = transactionPort.save(transaction);

    // 4. Create & Save Journal Entry
    JournalEntry journalEntry =
        JournalEntry.createCredit(savedTransaction.getId(), account.getId(), command.amount(), now);
    journalEntryPort.save(journalEntry);

    // 5. Update Balance
    Balance newBalance = balance.deposit(command.amount(), savedTransaction.getId(), now);
    balancePort.update(newBalance);

    // 6. Publish Domain Event
    outboxEventRecorder.record(
        savedTransaction.toPostedEvent(
            command.accountId(), command.amount(), TransactionType.DEPOSIT));
  }

  private boolean isDuplicateTransaction(String businessRefId) {
    return transactionPort.findByBusinessRefId(businessRefId).isPresent();
  }
}
