package com.securities.kuku.ledger.application.service;

import com.securities.kuku.ledger.application.port.in.WithdrawUseCase;
import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;
import com.securities.kuku.ledger.application.port.out.AccountPort;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionType;
import com.securities.kuku.ledger.domain.exception.InsufficientBalanceException;
import java.math.BigDecimal;
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
public class WithdrawService implements WithdrawUseCase {

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
  public void withdraw(WithdrawCommand command) {
    if (isDuplicateTransaction(command.businessRefId())) {
      log.warn("Duplicate transaction detected. businessRefId={}", command.businessRefId());
      return;
    }

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

    Instant now = clock.instant();
    validateSufficientBalance(balance, command.amount());

    Transaction transaction =
        Transaction.createWithdraw(command.description(), command.businessRefId(), now);
    Transaction savedTransaction = transactionPort.save(transaction);

    JournalEntry journalEntry =
        JournalEntry.createDebit(savedTransaction.getId(), account.getId(), command.amount(), now);
    journalEntryPort.save(journalEntry);
    Balance newBalance = balance.withdraw(command.amount(), savedTransaction.getId(), now);
    balancePort.update(newBalance);

    outboxEventRecorder.record(
        savedTransaction.toPostedEvent(
            command.accountId(), command.amount(), TransactionType.WITHDRAWAL));
  }

  private void validateSufficientBalance(Balance balance, BigDecimal amount) {
    if (amount.compareTo(balance.getAvailableAmount()) > 0) {
      throw new InsufficientBalanceException(
          balance.getAccountId(), amount, balance.getAvailableAmount());
    }
  }

  private boolean isDuplicateTransaction(String businessRefId) {
    return transactionPort.findByBusinessRefId(businessRefId).isPresent();
  }
}
