package com.securities.kuku.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.securities.kuku.ledger.application.port.in.command.ReversalCommand;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionStatus;
import com.securities.kuku.ledger.domain.TransactionType;
import com.securities.kuku.ledger.domain.event.LedgerReversedEvent;
import com.securities.kuku.ledger.domain.exception.InvalidTransactionStateException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReversalServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-12-09T15:00:00Z");
  private static final Long ORIGINAL_TX_ID = 100L;
  private static final Long ACCOUNT_ID = 1L;
  private static final String DEFAULT_REASON = "취소 요청";

  private ReversalService sut;
  private Clock fixedClock;

  private TransactionPort transactionPort;
  private JournalEntryPort journalEntryPort;
  private BalancePort balancePort;
  private OutboxEventRecorder outboxEventRecorder;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

    transactionPort = mock(TransactionPort.class);
    journalEntryPort = mock(JournalEntryPort.class);
    balancePort = mock(BalancePort.class);
    outboxEventRecorder = mock(OutboxEventRecorder.class);

    sut =
        new ReversalService(
            fixedClock, transactionPort, journalEntryPort, balancePort, outboxEventRecorder);
  }

  @Test
  @DisplayName("성공 시 원 트랜잭션이 REVERSED 상태로 변경된다")
  void success_changesOriginalTransactionToReversed() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().update(txCaptor.capture());
    assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.REVERSED);
  }

  @Test
  @DisplayName("성공 시 Outbox에 LedgerReversedEvent가 기록된다")
  void success_recordsOutboxEvent() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    then(outboxEventRecorder).should().record(any(LedgerReversedEvent.class));
  }

  @Test
  @DisplayName("성공 시 Outbox 이벤트에 올바른 originalTransactionId가 포함된다")
  void success_recordsOutboxEventWithCorrectOriginalTransactionId() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<LedgerReversedEvent> eventCaptor =
        ArgumentCaptor.forClass(LedgerReversedEvent.class);
    then(outboxEventRecorder).should().record(eventCaptor.capture());
    assertThat(eventCaptor.getValue().originalTransactionId()).isEqualTo(ORIGINAL_TX_ID);
  }

  @Test
  @DisplayName("성공 시 Outbox 이벤트에 올바른 사유가 포함된다")
  void success_recordsOutboxEventWithCorrectReason() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<LedgerReversedEvent> eventCaptor =
        ArgumentCaptor.forClass(LedgerReversedEvent.class);
    then(outboxEventRecorder).should().record(eventCaptor.capture());
    assertThat(eventCaptor.getValue().reason()).isEqualTo(DEFAULT_REASON);
  }

  @Test
  @DisplayName("성공 시 REVERSAL 타입의 트랜잭션이 생성된다")
  void success_createsReversalTypeTransaction() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().save(txCaptor.capture());
    assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.REVERSAL);
  }

  @Test
  @DisplayName("성공 시 역분개 트랜잭션에 원 트랜잭션 ID가 저장된다")
  void success_createsReversalTransactionWithOriginalId() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionPort).should().save(txCaptor.capture());
    assertThat(txCaptor.getValue().getReversalOfTransactionId()).isEqualTo(ORIGINAL_TX_ID);
  }

  @Test
  @DisplayName("성공 시 반대 분개가 배치로 저장된다")
  @SuppressWarnings("unchecked")
  void success_createsOppositeJournalEntriesInBatch() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Collection<JournalEntry>> journalCaptor =
        ArgumentCaptor.forClass(Collection.class);
    then(journalEntryPort).should().saveAll(journalCaptor.capture());
    assertThat(journalCaptor.getValue()).hasSize(1);
  }

  @Test
  @DisplayName("성공 시 원본 CREDIT 분개에 대해 DEBIT 역분개가 생성된다")
  @SuppressWarnings("unchecked")
  void success_createsDebitEntryForOriginalCredit() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Collection<JournalEntry>> journalCaptor =
        ArgumentCaptor.forClass(Collection.class);
    then(journalEntryPort).should().saveAll(journalCaptor.capture());
    JournalEntry oppositeEntry = journalCaptor.getValue().iterator().next();
    assertThat(oppositeEntry.getEntryType()).isEqualTo(JournalEntry.EntryType.DEBIT);
  }

  @Test
  @DisplayName("성공 시 잔액이 배치로 복구된다")
  @SuppressWarnings("unchecked")
  void success_restoresBalancesInBatch() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Collection<Balance>> balanceCaptor = ArgumentCaptor.forClass(Collection.class);
    then(balancePort).should().updateAll(balanceCaptor.capture());
    assertThat(balanceCaptor.getValue()).hasSize(1);
  }

  @Test
  @DisplayName("성공 시 잔액이 원래 값으로 복구된다")
  @SuppressWarnings("unchecked")
  void success_restoresBalanceToOriginalAmount() {
    // Given
    ReversalCommand command = createDefaultCommand();
    setupSuccessScenario();

    // When
    sut.reverse(command);

    // Then
    ArgumentCaptor<Collection<Balance>> balanceCaptor = ArgumentCaptor.forClass(Collection.class);
    then(balancePort).should().updateAll(balanceCaptor.capture());
    Balance restoredBalance = balanceCaptor.getValue().iterator().next();
    // Original deposit of 1000, reverse should withdraw -> 1000 - 1000 = 0
    assertThat(restoredBalance.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("존재하지 않는 트랜잭션이면 예외가 발생한다")
  void throwsException_whenTransactionNotFound() {
    // Given
    Long nonExistentTxId = 999L;
    ReversalCommand command = ReversalCommand.of(nonExistentTxId, DEFAULT_REASON);
    given(transactionPort.findById(nonExistentTxId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> sut.reverse(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Transaction not found");
  }

  @Test
  @DisplayName("이미 REVERSED된 트랜잭션이면 예외가 발생한다")
  void throwsException_whenTransactionAlreadyReversed() {
    // Given
    ReversalCommand command = createDefaultCommand();
    Transaction reversedTx = createTransactionWithStatus(TransactionStatus.REVERSED);
    given(transactionPort.findById(ORIGINAL_TX_ID)).willReturn(Optional.of(reversedTx));

    // When & Then
    assertThatThrownBy(() -> sut.reverse(command))
        .isInstanceOf(InvalidTransactionStateException.class)
        .hasMessageContaining("already reversed");
  }

  @Test
  @DisplayName("PENDING 상태 트랜잭션이면 예외가 발생한다")
  void throwsException_whenTransactionIsPending() {
    // Given
    ReversalCommand command = createDefaultCommand();
    Transaction pendingTx = createTransactionWithStatus(TransactionStatus.PENDING);
    given(transactionPort.findById(ORIGINAL_TX_ID)).willReturn(Optional.of(pendingTx));

    // When & Then
    assertThatThrownBy(() -> sut.reverse(command))
        .isInstanceOf(InvalidTransactionStateException.class)
        .hasMessageContaining("PENDING");
  }

  @Test
  @DisplayName("분개 목록이 비어있으면 데이터 정합성 예외가 발생한다")
  void throwsException_whenNoJournalEntries() {
    // Given
    ReversalCommand command = createDefaultCommand();
    Transaction originalTx = createTransactionWithStatus(TransactionStatus.POSTED);
    given(transactionPort.findById(ORIGINAL_TX_ID)).willReturn(Optional.of(originalTx));
    given(journalEntryPort.findByTransactionId(ORIGINAL_TX_ID)).willReturn(Collections.emptyList());

    // When & Then
    assertThatThrownBy(() -> sut.reverse(command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No journal entries found");
  }

  @Test
  @DisplayName("분개 목록이 비어있으면 데이터 정합성 문제를 메시지에 포함한다")
  void throwsException_withDataIntegrityMessage_whenNoJournalEntries() {
    // Given
    ReversalCommand command = createDefaultCommand();
    Transaction originalTx = createTransactionWithStatus(TransactionStatus.POSTED);
    given(transactionPort.findById(ORIGINAL_TX_ID)).willReturn(Optional.of(originalTx));
    given(journalEntryPort.findByTransactionId(ORIGINAL_TX_ID)).willReturn(Collections.emptyList());

    // When & Then
    assertThatThrownBy(() -> sut.reverse(command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Data integrity issue");
  }

  private ReversalCommand createDefaultCommand() {
    return ReversalCommand.of(ORIGINAL_TX_ID, DEFAULT_REASON);
  }

  private Transaction createTransactionWithStatus(TransactionStatus status) {
    return new Transaction(
        ORIGINAL_TX_ID, TransactionType.DEPOSIT, "입금", "ref-123", status, null, FIXED_TIME);
  }

  private void setupSuccessScenario() {
    // Original POSTED DEPOSIT transaction
    Transaction originalTx = createTransactionWithStatus(TransactionStatus.POSTED);
    given(transactionPort.findById(ORIGINAL_TX_ID)).willReturn(Optional.of(originalTx));

    // Original CREDIT journal entry (deposit)
    JournalEntry originalEntry =
        new JournalEntry(
            1L,
            ORIGINAL_TX_ID,
            ACCOUNT_ID,
            new BigDecimal("1000"),
            JournalEntry.EntryType.CREDIT,
            FIXED_TIME);
    given(journalEntryPort.findByTransactionId(ORIGINAL_TX_ID)).willReturn(List.of(originalEntry));

    // Batch load balances - use mutable HashMap
    Balance balance =
        new Balance(
            ACCOUNT_ID, new BigDecimal("1000"), BigDecimal.ZERO, 1L, ORIGINAL_TX_ID, FIXED_TIME);
    Map<Long, Balance> balanceMap = new HashMap<>();
    balanceMap.put(ACCOUNT_ID, balance);
    given(balancePort.findByAccountIds(anySet())).willReturn(balanceMap);

    // Mock save transaction to return with ID
    given(transactionPort.save(any()))
        .willAnswer(
            invocation -> {
              Transaction tx = invocation.getArgument(0);
              return new Transaction(
                  200L,
                  tx.getType(),
                  tx.getDescription(),
                  tx.getBusinessRefId(),
                  tx.getStatus(),
                  tx.getReversalOfTransactionId(),
                  tx.getCreatedAt());
            });
  }
}
