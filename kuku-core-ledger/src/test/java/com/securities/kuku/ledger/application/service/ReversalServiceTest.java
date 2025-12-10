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
import com.securities.kuku.ledger.domain.InvalidTransactionStateException;
import com.securities.kuku.ledger.domain.JournalEntry;
import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionStatus;
import com.securities.kuku.ledger.domain.TransactionType;
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

        private ReversalService sut;
        private Clock fixedClock;

        private TransactionPort transactionPort;
        private JournalEntryPort journalEntryPort;
        private BalancePort balancePort;

        @BeforeEach
        void setUp() {
                fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

                transactionPort = mock(TransactionPort.class);
                journalEntryPort = mock(JournalEntryPort.class);
                balancePort = mock(BalancePort.class);

                sut = new ReversalService(
                                fixedClock,
                                transactionPort,
                                journalEntryPort,
                                balancePort);
        }

        @Test
        @DisplayName("역분개 성공 시 원 트랜잭션이 REVERSED 상태로 변경되어야 한다")
        void reverse_ShouldChangeOriginalTransactionToReversed() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                setupSuccessScenario();

                // When
                sut.reverse(command);

                // Then
                ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
                then(transactionPort).should().update(txCaptor.capture());
                assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.REVERSED);
        }

        @Test
        @DisplayName("역분개 성공 시 역분개 트랜잭션이 생성되어야 한다")
        void reverse_ShouldCreateReversalTransaction() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                setupSuccessScenario();

                // When
                sut.reverse(command);

                // Then
                ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
                then(transactionPort).should().save(txCaptor.capture());
                Transaction reversalTx = txCaptor.getValue();
                assertThat(reversalTx.getType()).isEqualTo(TransactionType.REVERSAL);
                assertThat(reversalTx.getReversalOfTransactionId()).isEqualTo(ORIGINAL_TX_ID);
        }

        @Test
        @DisplayName("역분개 성공 시 반대 분개가 배치로 저장되어야 한다")
        @SuppressWarnings("unchecked")
        void reverse_ShouldCreateOppositeJournalEntriesInBatch() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                setupSuccessScenario();

                // When
                sut.reverse(command);

                // Then
                ArgumentCaptor<Collection<JournalEntry>> journalCaptor = ArgumentCaptor.forClass(Collection.class);
                then(journalEntryPort).should().saveAll(journalCaptor.capture());
                Collection<JournalEntry> savedEntries = journalCaptor.getValue();
                assertThat(savedEntries).hasSize(1);
                JournalEntry oppositeEntry = savedEntries.iterator().next();
                // Original was CREDIT, opposite should be DEBIT
                assertThat(oppositeEntry.getEntryType()).isEqualTo(JournalEntry.EntryType.DEBIT);
        }

        @Test
        @DisplayName("역분개 성공 시 잔액이 배치로 복구되어야 한다")
        @SuppressWarnings("unchecked")
        void reverse_ShouldRestoreBalancesInBatch() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                setupSuccessScenario();

                // When
                sut.reverse(command);

                // Then
                ArgumentCaptor<Collection<Balance>> balanceCaptor = ArgumentCaptor.forClass(Collection.class);
                then(balancePort).should().updateAll(balanceCaptor.capture());
                Collection<Balance> updatedBalances = balanceCaptor.getValue();
                assertThat(updatedBalances).hasSize(1);
                // Original deposit of 1000, reverse should withdraw -> 1000 - 1000 = 0
                Balance restoredBalance = updatedBalances.iterator().next();
                assertThat(restoredBalance.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("존재하지 않는 트랜잭션 역분개 시 예외가 발생해야 한다")
        void reverse_ShouldThrowException_WhenTransactionNotFound() {
                // Given
                ReversalCommand command = ReversalCommand.of(999L, "취소 요청");
                given(transactionPort.findById(999L)).willReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> sut.reverse(command))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("이미 REVERSED된 트랜잭션 역분개 시 예외가 발생해야 한다")
        void reverse_ShouldThrowException_WhenTransactionAlreadyReversed() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                Transaction reversedTx = new Transaction(
                                ORIGINAL_TX_ID,
                                TransactionType.DEPOSIT,
                                "입금",
                                "ref-123",
                                TransactionStatus.REVERSED,
                                null,
                                FIXED_TIME);
                given(transactionPort.findById(ORIGINAL_TX_ID))
                                .willReturn(Optional.of(reversedTx));

                // When & Then
                assertThatThrownBy(() -> sut.reverse(command))
                                .isInstanceOf(InvalidTransactionStateException.class)
                                .hasMessageContaining("already reversed");
        }

        @Test
        @DisplayName("PENDING 상태 트랜잭션 역분개 시 예외가 발생해야 한다")
        void reverse_ShouldThrowException_WhenTransactionIsPending() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                Transaction pendingTx = new Transaction(
                                ORIGINAL_TX_ID,
                                TransactionType.DEPOSIT,
                                "입금",
                                "ref-123",
                                TransactionStatus.PENDING,
                                null,
                                FIXED_TIME);
                given(transactionPort.findById(ORIGINAL_TX_ID))
                                .willReturn(Optional.of(pendingTx));

                // When & Then
                assertThatThrownBy(() -> sut.reverse(command))
                                .isInstanceOf(InvalidTransactionStateException.class)
                                .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("분개 목록이 비어있으면 데이터 정합성 예외가 발생해야 한다")
        void reverse_ShouldThrowException_WhenNoJournalEntries() {
                // Given
                ReversalCommand command = ReversalCommand.of(ORIGINAL_TX_ID, "취소 요청");
                Transaction originalTx = new Transaction(
                                ORIGINAL_TX_ID,
                                TransactionType.DEPOSIT,
                                "입금",
                                "ref-123",
                                TransactionStatus.POSTED,
                                null,
                                FIXED_TIME);
                given(transactionPort.findById(ORIGINAL_TX_ID)).willReturn(Optional.of(originalTx));
                given(journalEntryPort.findByTransactionId(ORIGINAL_TX_ID)).willReturn(Collections.emptyList());

                // When & Then
                assertThatThrownBy(() -> sut.reverse(command))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("No journal entries found")
                                .hasMessageContaining("Data integrity issue");
        }

        private void setupSuccessScenario() {
                // Original POSTED DEPOSIT transaction
                Transaction originalTx = new Transaction(
                                ORIGINAL_TX_ID,
                                TransactionType.DEPOSIT,
                                "입금",
                                "ref-123",
                                TransactionStatus.POSTED,
                                null,
                                FIXED_TIME);
                given(transactionPort.findById(ORIGINAL_TX_ID))
                                .willReturn(Optional.of(originalTx));

                // Original CREDIT journal entry (deposit)
                JournalEntry originalEntry = new JournalEntry(
                                1L,
                                ORIGINAL_TX_ID,
                                ACCOUNT_ID,
                                new BigDecimal("1000"),
                                JournalEntry.EntryType.CREDIT,
                                FIXED_TIME);
                given(journalEntryPort.findByTransactionId(ORIGINAL_TX_ID))
                                .willReturn(List.of(originalEntry));

                // Batch load balances - use mutable HashMap
                Balance balance = new Balance(
                                ACCOUNT_ID,
                                new BigDecimal("1000"),
                                BigDecimal.ZERO,
                                1L,
                                ORIGINAL_TX_ID,
                                FIXED_TIME);
                Map<Long, Balance> balanceMap = new HashMap<>();
                balanceMap.put(ACCOUNT_ID, balance);
                given(balancePort.findByAccountIds(anySet())).willReturn(balanceMap);

                // Mock save transaction to return with ID
                given(transactionPort.save(any())).willAnswer(invocation -> {
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
