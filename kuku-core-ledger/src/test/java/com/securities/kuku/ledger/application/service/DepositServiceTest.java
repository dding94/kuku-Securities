package com.securities.kuku.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.securities.kuku.ledger.application.port.in.command.DepositCommand;
import com.securities.kuku.ledger.application.port.out.AccountPort;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.AccountType;
import com.securities.kuku.ledger.domain.Balance;
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

class DepositServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2024-01-01T01:00:00Z");

  private DepositService sut;
  private Clock fixedClock;

  private AccountPort accountPort;
  private BalancePort balancePort;
  private TransactionPort transactionPort;
  private JournalEntryPort journalEntryPort;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

    accountPort = mock(AccountPort.class);
    balancePort = mock(BalancePort.class);
    transactionPort = mock(TransactionPort.class);
    journalEntryPort = mock(JournalEntryPort.class);

    sut =
        new DepositService(fixedClock, accountPort, balancePort, transactionPort, journalEntryPort);
  }

  @Test
  @DisplayName("입금 성공 시 POSTED 상태의 트랜잭션이 저장되어야 한다")
  void deposit_ShouldSavePostedTransaction() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("1000");
    String businessRefId = "ref-123";
    DepositCommand command = DepositCommand.of(accountId, amount, "Deposit", businessRefId);

    setupAccountAndBalance(accountId);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().save(txCaptor.capture());
    Transaction savedTx = txCaptor.getValue();
    assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.POSTED);
    assertThat(savedTx.getBusinessRefId()).isEqualTo(businessRefId);
  }

  @Test
  @DisplayName("입금 성공 시 잔액이 증가하여 업데이트되어야 한다")
  void deposit_ShouldUpdateBalance() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("1000");
    String businessRefId = "ref-123";
    DepositCommand command = DepositCommand.of(accountId, amount, "Deposit", businessRefId);

    setupAccountAndBalance(accountId);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
    then(balancePort).should().update(balanceCaptor.capture());
    Balance updatedBalance = balanceCaptor.getValue();
    assertThat(updatedBalance.getAmount()).isEqualByComparingTo(new BigDecimal("1000")); // 0 + 1000
  }

  @Test
  @DisplayName("입금 성공 시 CREDIT 유형의 분개가 저장되어야 한다")
  void deposit_ShouldSaveCreditJournalEntry() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("1000");
    String businessRefId = "ref-123";
    DepositCommand command = DepositCommand.of(accountId, amount, "Deposit", businessRefId);

    setupAccountAndBalance(accountId);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
    then(journalEntryPort).should().save(journalCaptor.capture());
    JournalEntry savedJournal = journalCaptor.getValue();
    assertThat(savedJournal.getAmount()).isEqualByComparingTo(amount);
    assertThat(savedJournal.getEntryType()).isEqualTo(JournalEntry.EntryType.CREDIT);
    assertThat(savedJournal.getAccountId()).isEqualTo(accountId);
  }

  @Test
  @DisplayName("존재하지 않는 계좌로 입금 시 예외가 발생해야 한다")
  void deposit_ShouldThrowException_WhenAccountNotFound() {
    // Given
    Long accountId = 999L;
    BigDecimal amount = new BigDecimal("1000");
    String businessRefId = "ref-123";
    DepositCommand command = DepositCommand.of(accountId, amount, "Deposit", businessRefId);

    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.empty());
    given(accountPort.findById(accountId)).willReturn(Optional.empty());

    // When & Then
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          sut.deposit(command);
        });
  }

  @Test
  @DisplayName("이미 처리된 비즈니스 ID로 입금 요청 시 아무 작업도 수행하지 않아야 한다 (멱등성)")
  void deposit_ShouldDoNothing_WhenTransactionExists() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("1000");
    String businessRefId = "ref-123";
    DepositCommand command = DepositCommand.of(accountId, amount, "Deposit", businessRefId);

    Transaction existingTx =
        new Transaction(
            1L,
            TransactionType.DEPOSIT,
            "Existing",
            businessRefId,
            TransactionStatus.POSTED,
            null,
            FIXED_TIME);
    given(transactionPort.findByBusinessRefId(businessRefId)).willReturn(Optional.of(existingTx));

    // When
    sut.deposit(command);

    // Then
    then(transactionPort).should().findByBusinessRefId(businessRefId);
    then(transactionPort).shouldHaveNoMoreInteractions();
    then(balancePort).shouldHaveNoInteractions();
    then(journalEntryPort).shouldHaveNoInteractions();
  }

  private void setupAccountAndBalance(Long accountId) {
    Account account =
        new Account(
            accountId,
            100L, // userId
            "123-456",
            "KRW",
            AccountType.USER_CASH,
            FIXED_TIME);
    Balance balance =
        new Balance(
            accountId,
            BigDecimal.ZERO,
            BigDecimal.ZERO, // holdAmount
            0L, // version
            0L, // lastTransactionId
            FIXED_TIME);

    given(accountPort.findById(accountId)).willReturn(Optional.of(account));
    given(balancePort.findByAccountId(accountId)).willReturn(Optional.of(balance));

    given(transactionPort.save(any()))
        .willAnswer(
            invocation -> {
              Transaction tx = invocation.getArgument(0);
              return new Transaction(
                  999L, // Simulated Generated ID
                  tx.getType(),
                  tx.getDescription(),
                  tx.getBusinessRefId(),
                  tx.getStatus(),
                  tx.getReversalOfTransactionId(),
                  tx.getCreatedAt());
            });
  }
}
