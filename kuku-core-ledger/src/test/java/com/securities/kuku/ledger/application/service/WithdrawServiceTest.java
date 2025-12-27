package com.securities.kuku.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;
import com.securities.kuku.ledger.application.port.out.AccountPort;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.AccountType;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.InsufficientBalanceException;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionStatus;
import com.securities.kuku.ledger.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WithdrawServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-12-09T15:00:00Z");

  private WithdrawService sut;
  private Clock fixedClock;

  private AccountPort accountPort;
  private BalancePort balancePort;
  private TransactionPort transactionPort;
  private JournalEntryPort journalEntryPort;
  private OutboxEventRecorder outboxEventRecorder;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

    accountPort = mock(AccountPort.class);
    balancePort = mock(BalancePort.class);
    transactionPort = mock(TransactionPort.class);
    journalEntryPort = mock(JournalEntryPort.class);
    outboxEventRecorder = mock(OutboxEventRecorder.class);

    sut =
        new WithdrawService(
            fixedClock,
            accountPort,
            balancePort,
            transactionPort,
            journalEntryPort,
            outboxEventRecorder);
  }

  @Test
  @DisplayName("출금 성공 시 POSTED 상태의 WITHDRAWAL 트랜잭션이 저장되어야 한다")
  void withdraw_ShouldSavePostedWithdrawalTransaction() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

    setupAccountAndBalance(accountId, new BigDecimal("1000"));
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    sut.withdraw(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().save(txCaptor.capture());
    Transaction savedTx = txCaptor.getValue();
    assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.POSTED);
    assertThat(savedTx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    assertThat(savedTx.getBusinessRefId()).isEqualTo(businessRefId);
  }

  @Test
  @DisplayName("출금 성공 시 잔액이 감소하여 업데이트되어야 한다")
  void withdraw_ShouldDecreaseBalance() {
    // Given
    Long accountId = 1L;
    BigDecimal initialBalance = new BigDecimal("1000");
    BigDecimal withdrawAmount = new BigDecimal("300");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command =
        WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

    setupAccountAndBalance(accountId, initialBalance);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    sut.withdraw(command);

    // Then
    ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
    then(balancePort).should().update(balanceCaptor.capture());
    Balance updatedBalance = balanceCaptor.getValue();
    assertThat(updatedBalance.getAmount()).isEqualByComparingTo(new BigDecimal("700"));
  }

  @Test
  @DisplayName("출금 성공 시 DEBIT 유형의 분개가 저장되어야 한다")
  void withdraw_ShouldSaveDebitJournalEntry() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

    setupAccountAndBalance(accountId, new BigDecimal("1000"));
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    sut.withdraw(command);

    // Then
    ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
    then(journalEntryPort).should().save(journalCaptor.capture());
    JournalEntry savedJournal = journalCaptor.getValue();
    assertThat(savedJournal.getAmount()).isEqualByComparingTo(amount);
    assertThat(savedJournal.getEntryType()).isEqualTo(JournalEntry.EntryType.DEBIT);
    assertThat(savedJournal.getAccountId()).isEqualTo(accountId);
  }

  @Test
  @DisplayName("잔액이 부족하면 InsufficientBalanceException이 발생해야 한다")
  void withdraw_ShouldThrowException_WhenInsufficientBalance() {
    // Given
    Long accountId = 1L;
    BigDecimal currentBalance = new BigDecimal("100");
    BigDecimal withdrawAmount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command =
        WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

    setupAccountAndBalance(accountId, currentBalance);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When & Then
    org.junit.jupiter.api.Assertions.assertThrows(
        InsufficientBalanceException.class,
        () -> {
          sut.withdraw(command);
        });
  }

  @Test
  @DisplayName("잔액 부족 시 트랜잭션이 저장되지 않아야 한다")
  void withdraw_ShouldNotSaveTransaction_WhenInsufficientBalance() {
    // Given
    Long accountId = 1L;
    BigDecimal currentBalance = new BigDecimal("100");
    BigDecimal withdrawAmount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command =
        WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

    setupAccountAndBalance(accountId, currentBalance);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    catchThrowable(() -> sut.withdraw(command));

    // Then
    then(transactionPort).should().findByBusinessRefId(businessRefId);
    then(transactionPort).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("잔액 부족 시 잔액이 업데이트되지 않아야 한다")
  void withdraw_ShouldNotUpdateBalance_WhenInsufficientBalance() {
    // Given
    Long accountId = 1L;
    BigDecimal currentBalance = new BigDecimal("100");
    BigDecimal withdrawAmount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command =
        WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

    setupAccountAndBalance(accountId, currentBalance);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    catchThrowable(() -> sut.withdraw(command));

    // Then
    then(balancePort).should().findByAccountId(accountId);
    then(balancePort).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("존재하지 않는 계좌로 출금 시 예외가 발생해야 한다")
  void withdraw_ShouldThrowException_WhenAccountNotFound() {
    // Given
    Long accountId = 999L;
    BigDecimal amount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());
    given(accountPort.findById(accountId)).willReturn(Optional.empty());

    // When & Then
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          sut.withdraw(command);
        });
  }

  @Test
  @DisplayName("중복 요청 시 트랜잭션이 저장되지 않아야 한다 (멱등성)")
  void withdraw_ShouldNotSaveTransaction_WhenDuplicateRequest() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

    Transaction existingTx =
        new Transaction(
            1L,
            TransactionType.WITHDRAWAL,
            "Existing",
            businessRefId,
            TransactionStatus.POSTED,
            null,
            FIXED_TIME);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.of(existingTx));

    // When
    sut.withdraw(command);

    // Then
    then(transactionPort).should().findByBusinessRefId(businessRefId);
    then(transactionPort).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("중복 요청 시 잔액이 업데이트되지 않아야 한다 (멱등성)")
  void withdraw_ShouldNotUpdateBalance_WhenDuplicateRequest() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

    Transaction existingTx =
        new Transaction(
            1L,
            TransactionType.WITHDRAWAL,
            "Existing",
            businessRefId,
            TransactionStatus.POSTED,
            null,
            FIXED_TIME);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.of(existingTx));

    // When
    sut.withdraw(command);

    // Then
    then(balancePort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("중복 요청 시 분개가 저장되지 않아야 한다 (멱등성)")
  void withdraw_ShouldNotSaveJournalEntry_WhenDuplicateRequest() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("500");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

    Transaction existingTx =
        new Transaction(
            1L,
            TransactionType.WITHDRAWAL,
            "Existing",
            businessRefId,
            TransactionStatus.POSTED,
            null,
            FIXED_TIME);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.of(existingTx));

    // When
    sut.withdraw(command);

    // Then
    then(journalEntryPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("holdAmount를 고려한 가용 잔액이 부족하면 예외가 발생해야 한다")
  void withdraw_ShouldThrowException_WhenAvailableBalanceInsufficient() {
    // Given
    Long accountId = 1L;
    BigDecimal totalAmount = new BigDecimal("1000");
    BigDecimal holdAmount = new BigDecimal("300");
    BigDecimal withdrawAmount = new BigDecimal("800");
    String businessRefId = "withdraw-ref-123";
    WithdrawCommand command =
        WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

    setupAccountAndBalanceWithHold(accountId, totalAmount, holdAmount);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When & Then
    org.junit.jupiter.api.Assertions.assertThrows(
        InsufficientBalanceException.class,
        () -> {
          sut.withdraw(command);
        });
  }

  private void setupAccountAndBalance(Long accountId, BigDecimal amount) {
    Account account =
        new Account(accountId, 100L, "123-456", "KRW", AccountType.USER_CASH, FIXED_TIME);
    Balance balance = new Balance(accountId, amount, BigDecimal.ZERO, 0L, 0L, FIXED_TIME);

    given(accountPort.findById(accountId)).willReturn(Optional.of(account));
    given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

    given(transactionPort.save(any()))
        .willAnswer(
            invocation -> {
              Transaction tx = invocation.getArgument(0);
              return new Transaction(
                  999L,
                  tx.getType(),
                  tx.getDescription(),
                  tx.getBusinessRefId(),
                  tx.getStatus(),
                  tx.getReversalOfTransactionId(),
                  tx.getCreatedAt());
            });
  }

  private void setupAccountAndBalanceWithHold(
      Long accountId, BigDecimal amount, BigDecimal holdAmount) {
    Account account =
        new Account(accountId, 100L, "123-456", "KRW", AccountType.USER_CASH, FIXED_TIME);
    Balance balance = new Balance(accountId, amount, holdAmount, 0L, 0L, FIXED_TIME);

    given(accountPort.findById(accountId)).willReturn(Optional.of(account));
    given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

    given(transactionPort.save(any()))
        .willAnswer(
            invocation -> {
              Transaction tx = invocation.getArgument(0);
              return new Transaction(
                  999L,
                  tx.getType(),
                  tx.getDescription(),
                  tx.getBusinessRefId(),
                  tx.getStatus(),
                  tx.getReversalOfTransactionId(),
                  tx.getCreatedAt());
            });
  }
}
