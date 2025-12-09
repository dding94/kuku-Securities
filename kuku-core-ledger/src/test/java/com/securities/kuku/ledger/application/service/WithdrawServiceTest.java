package com.securities.kuku.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;
import com.securities.kuku.ledger.application.port.out.LoadAccountPort;
import com.securities.kuku.ledger.application.port.out.LoadBalancePort;
import com.securities.kuku.ledger.application.port.out.LoadTransactionPort;
import com.securities.kuku.ledger.application.port.out.SaveJournalEntryPort;
import com.securities.kuku.ledger.application.port.out.SaveTransactionPort;
import com.securities.kuku.ledger.application.port.out.UpdateBalancePort;
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WithdrawServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2024-01-01T01:00:00Z");

    private WithdrawService sut;
    private Clock fixedClock;

    private LoadAccountPort loadAccountPort;
    private LoadBalancePort loadBalancePort;
    private SaveTransactionPort saveTransactionPort;
    private SaveJournalEntryPort saveJournalEntryPort;
    private UpdateBalancePort updateBalancePort;
    private LoadTransactionPort loadTransactionPort;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

        loadAccountPort = mock(LoadAccountPort.class);
        loadBalancePort = mock(LoadBalancePort.class);
        saveTransactionPort = mock(SaveTransactionPort.class);
        saveJournalEntryPort = mock(SaveJournalEntryPort.class);
        updateBalancePort = mock(UpdateBalancePort.class);
        loadTransactionPort = mock(LoadTransactionPort.class);

        sut = new WithdrawService(
                fixedClock,
                loadAccountPort,
                loadBalancePort,
                saveTransactionPort,
                saveJournalEntryPort,
                updateBalancePort,
                loadTransactionPort);
    }

    @Test
    @DisplayName("출금 성공 시 POSTED 상태의 WITHDRAWAL 트랜잭션이 저장되어야 한다")
    void withdraw_ShouldSavePostedTransaction() {
        // Given
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

        setupAccountAndBalance(accountId, new BigDecimal("1000"));
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When
        sut.withdraw(command);

        // Then
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        then(saveTransactionPort).should().saveTransaction(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThat(savedTx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(savedTx.getBusinessRefId()).isEqualTo(businessRefId);
    }

    @Test
    @DisplayName("출금 성공 시 잔액이 감소하여 업데이트되어야 한다")
    void withdraw_ShouldUpdateBalance() {
        // Given
        Long accountId = 1L;
        BigDecimal initialBalance = new BigDecimal("1000");
        BigDecimal withdrawAmount = new BigDecimal("300");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

        setupAccountAndBalance(accountId, initialBalance);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When
        sut.withdraw(command);

        // Then
        ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
        then(updateBalancePort).should().updateBalance(balanceCaptor.capture());
        Balance updatedBalance = balanceCaptor.getValue();
        assertThat(updatedBalance.getAmount()).isEqualByComparingTo(new BigDecimal("700")); // 1000 - 300
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
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When
        sut.withdraw(command);

        // Then
        ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        then(saveJournalEntryPort).should().saveJournalEntry(journalCaptor.capture());
        JournalEntry savedJournal = journalCaptor.getValue();
        assertThat(savedJournal.getAmount()).isEqualByComparingTo(amount);
        assertThat(savedJournal.getEntryType()).isEqualTo(JournalEntry.EntryType.DEBIT);
        assertThat(savedJournal.getAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("잔액 부족 시 InsufficientBalanceException이 발생해야 한다")
    void withdraw_ShouldThrowException_WhenInsufficientBalance() {
        // Given
        Long accountId = 1L;
        BigDecimal currentBalance = new BigDecimal("100");
        BigDecimal withdrawAmount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

        setupAccountAndBalance(accountId, currentBalance);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.withdraw(command))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("잔액 부족 시 트랜잭션이 저장되지 않아야 한다")
    void withdraw_ShouldNotSaveTransaction_WhenInsufficientBalance() {
        // Given
        Long accountId = 1L;
        BigDecimal currentBalance = new BigDecimal("100");
        BigDecimal withdrawAmount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

        setupAccountAndBalance(accountId, currentBalance);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When
        Throwable thrown = Assertions.catchThrowable(() -> sut.withdraw(command));

        // Then
        assertThat(thrown).isInstanceOf(InsufficientBalanceException.class);
        then(saveTransactionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잔액 부족 시 잔액이 업데이트되지 않아야 한다")
    void withdraw_ShouldNotUpdateBalance_WhenInsufficientBalance() {
        // Given
        Long accountId = 1L;
        BigDecimal currentBalance = new BigDecimal("100");
        BigDecimal withdrawAmount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

        setupAccountAndBalance(accountId, currentBalance);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When
        Throwable thrown = Assertions.catchThrowable(() -> sut.withdraw(command));

        // Then
        assertThat(thrown).isInstanceOf(InsufficientBalanceException.class);
        then(updateBalancePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 출금 시 예외가 발생해야 한다")
    void withdraw_ShouldThrowException_WhenAccountNotFound() {
        // Given
        Long accountId = 999L;
        BigDecimal amount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());
        given(loadAccountPort.loadAccount(accountId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.withdraw(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("이미 처리된 비즈니스 ID로 출금 요청 시 트랜잭션이 저장되지 않아야 한다 (멱등성)")
    void withdraw_ShouldNotSaveTransaction_WhenDuplicateRequest() {
        // Given
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

        Transaction existingTx = new Transaction(1L, TransactionType.WITHDRAWAL,
                "Existing", businessRefId, TransactionStatus.POSTED, null, FIXED_TIME);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.of(existingTx));

        // When
        sut.withdraw(command);

        // Then
        then(saveTransactionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 처리된 비즈니스 ID로 출금 요청 시 잔액이 업데이트되지 않아야 한다 (멱등성)")
    void withdraw_ShouldNotUpdateBalance_WhenDuplicateRequest() {
        // Given
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

        Transaction existingTx = new Transaction(1L, TransactionType.WITHDRAWAL,
                "Existing", businessRefId, TransactionStatus.POSTED, null, FIXED_TIME);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.of(existingTx));

        // When
        sut.withdraw(command);

        // Then
        then(updateBalancePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 처리된 비즈니스 ID로 출금 요청 시 분개가 저장되지 않아야 한다 (멱등성)")
    void withdraw_ShouldNotSaveJournalEntry_WhenDuplicateRequest() {
        // Given
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("500");
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, amount, "Withdraw", businessRefId);

        Transaction existingTx = new Transaction(1L, TransactionType.WITHDRAWAL,
                "Existing", businessRefId, TransactionStatus.POSTED, null, FIXED_TIME);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.of(existingTx));

        // When
        sut.withdraw(command);

        // Then
        then(saveJournalEntryPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("holdAmount가 있는 경우 가용잔액 기준으로 출금 가능 여부를 판단해야 한다")
    void withdraw_ShouldConsiderHoldAmount_WhenCheckingAvailableBalance() {
        // Given
        Long accountId = 1L;
        BigDecimal totalAmount = new BigDecimal("1000");
        BigDecimal holdAmount = new BigDecimal("300"); // Available = 1000 - 300 = 700
        BigDecimal withdrawAmount = new BigDecimal("800"); // Request more than available (700)
        String businessRefId = "withdraw-ref-123";
        WithdrawCommand command = WithdrawCommand.of(accountId, withdrawAmount, "Withdraw", businessRefId);

        setupAccountAndBalanceWithHold(accountId, totalAmount, holdAmount);
        given(loadTransactionPort.loadTransaction(businessRefId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.withdraw(command))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    private void setupAccountAndBalance(Long accountId, BigDecimal balanceAmount) {
        setupAccountAndBalanceWithHold(accountId, balanceAmount, BigDecimal.ZERO);
    }

    private void setupAccountAndBalanceWithHold(Long accountId, BigDecimal balanceAmount, BigDecimal holdAmount) {
        Account account = new Account(
                accountId,
                100L, // userId
                "123-456",
                "KRW",
                AccountType.USER_CASH,
                FIXED_TIME);
        Balance balance = new Balance(
                accountId,
                balanceAmount,
                holdAmount,
                0L, // version
                0L, // lastTransactionId
                FIXED_TIME);

        given(loadAccountPort.loadAccount(accountId)).willReturn(Optional.of(account));
        given(loadBalancePort.loadBalance(accountId)).willReturn(Optional.of(balance));

        given(saveTransactionPort.saveTransaction(any())).willAnswer(invocation -> {
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
