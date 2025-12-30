package com.securities.kuku.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.securities.kuku.ledger.domain.event.LedgerPostedEvent;
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
  private static final Long DEFAULT_ACCOUNT_ID = 1L;
  private static final BigDecimal DEFAULT_DEPOSIT_AMOUNT = new BigDecimal("1000");
  private static final String DEFAULT_BUSINESS_REF_ID = "ref-123";

  private DepositService sut;
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
        new DepositService(
            fixedClock,
            accountPort,
            balancePort,
            transactionPort,
            journalEntryPort,
            outboxEventRecorder);
  }

  @Test
  @DisplayName("성공 시 POSTED 상태의 트랜잭션이 저장된다")
  void success_savesPostedTransaction() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().save(txCaptor.capture());
    assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.POSTED);
  }

  @Test
  @DisplayName("성공 시 businessRefId가 트랜잭션에 저장된다")
  void success_savesTransactionWithBusinessRefId() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().save(txCaptor.capture());
    assertThat(txCaptor.getValue().getBusinessRefId()).isEqualTo(DEFAULT_BUSINESS_REF_ID);
  }

  @Test
  @DisplayName("성공 시 Outbox에 LedgerPostedEvent가 기록된다")
  void success_recordsOutboxEvent() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<LedgerPostedEvent> eventCaptor =
        ArgumentCaptor.forClass(LedgerPostedEvent.class);
    then(outboxEventRecorder).should().record(eventCaptor.capture());
    assertThat(eventCaptor.getValue().transactionType()).isEqualTo(TransactionType.DEPOSIT);
  }

  @Test
  @DisplayName("성공 시 Outbox 이벤트에 올바른 accountId가 포함된다")
  void success_recordsOutboxEventWithCorrectAccountId() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<LedgerPostedEvent> eventCaptor =
        ArgumentCaptor.forClass(LedgerPostedEvent.class);
    then(outboxEventRecorder).should().record(eventCaptor.capture());
    assertThat(eventCaptor.getValue().accountId()).isEqualTo(DEFAULT_ACCOUNT_ID);
  }

  @Test
  @DisplayName("성공 시 Outbox 이벤트에 올바른 금액이 포함된다")
  void success_recordsOutboxEventWithCorrectAmount() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<LedgerPostedEvent> eventCaptor =
        ArgumentCaptor.forClass(LedgerPostedEvent.class);
    then(outboxEventRecorder).should().record(eventCaptor.capture());
    assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo(DEFAULT_DEPOSIT_AMOUNT);
  }

  @Test
  @DisplayName("성공 시 잔액이 증가하여 업데이트된다")
  void success_updatesBalance() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
    then(balancePort).should().update(balanceCaptor.capture());
    assertThat(balanceCaptor.getValue().getAmount())
        .isEqualByComparingTo(new BigDecimal("1000")); // 0 + 1000
  }

  @Test
  @DisplayName("성공 시 CREDIT 유형의 분개가 저장된다")
  void success_savesCreditJournalEntry() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
    then(journalEntryPort).should().save(journalCaptor.capture());
    assertThat(journalCaptor.getValue().getEntryType()).isEqualTo(JournalEntry.EntryType.CREDIT);
  }

  @Test
  @DisplayName("성공 시 분개에 올바른 금액이 저장된다")
  void success_savesJournalEntryWithCorrectAmount() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
    then(journalEntryPort).should().save(journalCaptor.capture());
    assertThat(journalCaptor.getValue().getAmount()).isEqualByComparingTo(DEFAULT_DEPOSIT_AMOUNT);
  }

  @Test
  @DisplayName("성공 시 분개에 올바른 accountId가 저장된다")
  void success_savesJournalEntryWithCorrectAccountId() {
    // Given
    DepositCommand command = createDefaultCommand();
    setupAccountBalanceAndTransactionMock(DEFAULT_ACCOUNT_ID);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());

    // When
    sut.deposit(command);

    // Then
    ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
    then(journalEntryPort).should().save(journalCaptor.capture());
    assertThat(journalCaptor.getValue().getAccountId()).isEqualTo(DEFAULT_ACCOUNT_ID);
  }

  @Test
  @DisplayName("존재하지 않는 계좌면 예외가 발생한다")
  void throwsException_whenAccountNotFound() {
    // Given
    Long nonExistentAccountId = 999L;
    DepositCommand command =
        createCommand(nonExistentAccountId, DEFAULT_DEPOSIT_AMOUNT, DEFAULT_BUSINESS_REF_ID);

    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.empty());
    given(accountPort.findById(nonExistentAccountId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> sut.deposit(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Account not found");
  }

  @Test
  @DisplayName("이미 처리된 비즈니스 ID면 트랜잭션이 저장되지 않는다 (멱등성)")
  void doesNotSaveTransaction_whenDuplicateRequest() {
    // Given
    DepositCommand command = createDefaultCommand();
    Transaction existingTx =
        Transaction.createDeposit("Existing", DEFAULT_BUSINESS_REF_ID, FIXED_TIME);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.of(existingTx));

    // When
    sut.deposit(command);

    // Then
    then(transactionPort).should().findByBusinessRefId(DEFAULT_BUSINESS_REF_ID);
    then(transactionPort).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("이미 처리된 비즈니스 ID면 잔액이 업데이트되지 않는다 (멱등성)")
  void doesNotUpdateBalance_whenDuplicateRequest() {
    // Given
    DepositCommand command = createDefaultCommand();
    Transaction existingTx =
        Transaction.createDeposit("Existing", DEFAULT_BUSINESS_REF_ID, FIXED_TIME);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.of(existingTx));

    // When
    sut.deposit(command);

    // Then
    then(balancePort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("이미 처리된 비즈니스 ID면 분개가 저장되지 않는다 (멱등성)")
  void doesNotSaveJournalEntry_whenDuplicateRequest() {
    // Given
    DepositCommand command = createDefaultCommand();
    Transaction existingTx =
        Transaction.createDeposit("Existing", DEFAULT_BUSINESS_REF_ID, FIXED_TIME);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.of(existingTx));

    // When
    sut.deposit(command);

    // Then
    then(journalEntryPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("이미 처리된 비즈니스 ID면 Outbox 이벤트가 기록되지 않는다 (멱등성)")
  void doesNotRecordOutboxEvent_whenDuplicateRequest() {
    // Given
    DepositCommand command = createDefaultCommand();
    Transaction existingTx =
        Transaction.createDeposit("Existing", DEFAULT_BUSINESS_REF_ID, FIXED_TIME);
    given(transactionPort.findByBusinessRefId(DEFAULT_BUSINESS_REF_ID))
        .willReturn(Optional.of(existingTx));

    // When
    sut.deposit(command);

    // Then
    then(outboxEventRecorder).shouldHaveNoInteractions();
  }

  private DepositCommand createDefaultCommand() {
    return createCommand(DEFAULT_ACCOUNT_ID, DEFAULT_DEPOSIT_AMOUNT, DEFAULT_BUSINESS_REF_ID);
  }

  private DepositCommand createCommand(Long accountId, BigDecimal amount, String businessRefId) {
    return DepositCommand.of(accountId, amount, "Deposit", businessRefId);
  }

  private void setupAccountBalanceAndTransactionMock(Long accountId) {
    Account account =
        new Account(accountId, 100L, "123-456", "KRW", AccountType.USER_CASH, FIXED_TIME);
    Balance balance = new Balance(accountId, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, FIXED_TIME);

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
