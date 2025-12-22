package com.securities.kuku.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.securities.kuku.ledger.application.port.in.command.ConfirmTransactionCommand;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.InvalidTransactionStateException;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConfirmTransactionServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-01T03:00:00Z");

  private ConfirmTransactionService sut;
  private Clock fixedClock;

  private TransactionPort transactionPort;
  private BalancePort balancePort;
  private JournalEntryPort journalEntryPort;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

    transactionPort = mock(TransactionPort.class);
    balancePort = mock(BalancePort.class);
    journalEntryPort = mock(JournalEntryPort.class);

    sut = new ConfirmTransactionService(fixedClock, transactionPort, balancePort, journalEntryPort);
  }

  @Nested
  @DisplayName("Deposit 확정 성공")
  class DepositConfirmSuccess {

    @Test
    @DisplayName("PENDING DEPOSIT 트랜잭션 확정 시 POSTED로 전환된다")
    void confirm_ShouldUpdateTransactionToPosted() {
      // Given
      Long transactionId = 1L;
      Long accountId = 100L;
      BigDecimal amount = new BigDecimal("1000");
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, accountId, amount);

      Transaction pendingTx =
          new Transaction(
              transactionId,
              TransactionType.DEPOSIT,
              "입금",
              "REF-001",
              TransactionStatus.PENDING,
              null,
              FIXED_TIME);
      Balance balance =
          new Balance(accountId, BigDecimal.ZERO, BigDecimal.ZERO, 0L, null, FIXED_TIME);

      given(transactionPort.findById(transactionId)).willReturn(Optional.of(pendingTx));
      given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

      // When
      sut.confirm(command);

      // Then
      ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
      then(transactionPort).should().update(txCaptor.capture());
      assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.POSTED);
    }

    @Test
    @DisplayName("PENDING DEPOSIT 트랜잭션 확정 시 CREDIT JournalEntry가 저장된다")
    void confirm_ShouldSaveCreditJournalEntry() {
      // Given
      Long transactionId = 1L;
      Long accountId = 100L;
      BigDecimal amount = new BigDecimal("1000");
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, accountId, amount);

      Transaction pendingTx =
          new Transaction(
              transactionId,
              TransactionType.DEPOSIT,
              "입금",
              "REF-001",
              TransactionStatus.PENDING,
              null,
              FIXED_TIME);
      Balance balance =
          new Balance(accountId, BigDecimal.ZERO, BigDecimal.ZERO, 0L, null, FIXED_TIME);

      given(transactionPort.findById(transactionId)).willReturn(Optional.of(pendingTx));
      given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

      // When
      sut.confirm(command);

      // Then
      ArgumentCaptor<JournalEntry> jeCaptor = ArgumentCaptor.forClass(JournalEntry.class);
      then(journalEntryPort).should().save(jeCaptor.capture());
      assertThat(jeCaptor.getValue().getEntryType()).isEqualTo(JournalEntry.EntryType.CREDIT);
    }

    @Test
    @DisplayName("PENDING DEPOSIT 트랜잭션 확정 시 잔액이 증가한다")
    void confirm_ShouldIncreaseBalance() {
      // Given
      Long transactionId = 1L;
      Long accountId = 100L;
      BigDecimal amount = new BigDecimal("1000");
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, accountId, amount);

      Transaction pendingTx =
          new Transaction(
              transactionId,
              TransactionType.DEPOSIT,
              "입금",
              "REF-001",
              TransactionStatus.PENDING,
              null,
              FIXED_TIME);
      Balance balance =
          new Balance(accountId, new BigDecimal("500"), BigDecimal.ZERO, 0L, null, FIXED_TIME);

      given(transactionPort.findById(transactionId)).willReturn(Optional.of(pendingTx));
      given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

      // When
      sut.confirm(command);

      // Then
      ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
      then(balancePort).should().update(balanceCaptor.capture());
      assertThat(balanceCaptor.getValue().getAmount())
          .isEqualByComparingTo(new BigDecimal("1500")); // 500 + 1000
    }
  }

  @Nested
  @DisplayName("Withdraw 확정 성공")
  class WithdrawConfirmSuccess {

    @Test
    @DisplayName("PENDING WITHDRAWAL 트랜잭션 확정 시 DEBIT JournalEntry가 저장된다")
    void confirm_ShouldSaveDebitJournalEntry() {
      // Given
      Long transactionId = 1L;
      Long accountId = 100L;
      BigDecimal amount = new BigDecimal("500");
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, accountId, amount);

      Transaction pendingTx =
          new Transaction(
              transactionId,
              TransactionType.WITHDRAWAL,
              "출금",
              "REF-002",
              TransactionStatus.PENDING,
              null,
              FIXED_TIME);
      Balance balance =
          new Balance(accountId, new BigDecimal("1000"), BigDecimal.ZERO, 0L, null, FIXED_TIME);

      given(transactionPort.findById(transactionId)).willReturn(Optional.of(pendingTx));
      given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

      // When
      sut.confirm(command);

      // Then
      ArgumentCaptor<JournalEntry> jeCaptor = ArgumentCaptor.forClass(JournalEntry.class);
      then(journalEntryPort).should().save(jeCaptor.capture());
      assertThat(jeCaptor.getValue().getEntryType()).isEqualTo(JournalEntry.EntryType.DEBIT);
    }

    @Test
    @DisplayName("PENDING WITHDRAWAL 트랜잭션 확정 시 잔액이 감소한다")
    void confirm_ShouldDecreaseBalance() {
      // Given
      Long transactionId = 1L;
      Long accountId = 100L;
      BigDecimal amount = new BigDecimal("300");
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, accountId, amount);

      Transaction pendingTx =
          new Transaction(
              transactionId,
              TransactionType.WITHDRAWAL,
              "출금",
              "REF-002",
              TransactionStatus.PENDING,
              null,
              FIXED_TIME);
      Balance balance =
          new Balance(accountId, new BigDecimal("1000"), BigDecimal.ZERO, 0L, null, FIXED_TIME);

      given(transactionPort.findById(transactionId)).willReturn(Optional.of(pendingTx));
      given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

      // When
      sut.confirm(command);

      // Then
      ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
      then(balancePort).should().update(balanceCaptor.capture());
      assertThat(balanceCaptor.getValue().getAmount())
          .isEqualByComparingTo(new BigDecimal("700")); // 1000 - 300
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class FailureCases {

    @Test
    @DisplayName("Transaction이 존재하지 않으면 예외가 발생한다")
    void confirm_ShouldThrowException_WhenTransactionNotFound() {
      // Given
      Long transactionId = 999L;
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, 100L, new BigDecimal("1000"));

      given(transactionPort.findById(transactionId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> sut.confirm(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Transaction not found");
    }

    @Test
    @DisplayName("PENDING이 아닌 트랜잭션 확정 시 예외가 발생한다")
    void confirm_ShouldThrowException_WhenNotPending() {
      // Given
      Long transactionId = 1L;
      ConfirmTransactionCommand command =
          new ConfirmTransactionCommand(transactionId, 100L, new BigDecimal("1000"));

      Transaction postedTx =
          new Transaction(
              transactionId,
              TransactionType.DEPOSIT,
              "입금",
              "REF-001",
              TransactionStatus.POSTED,
              null,
              FIXED_TIME);

      given(transactionPort.findById(transactionId)).willReturn(Optional.of(postedTx));

      // When & Then
      assertThatThrownBy(() -> sut.confirm(command))
          .isInstanceOf(InvalidTransactionStateException.class)
          .hasMessageContaining("PENDING");

      then(transactionPort).should(never()).update(any());
    }
  }
}
