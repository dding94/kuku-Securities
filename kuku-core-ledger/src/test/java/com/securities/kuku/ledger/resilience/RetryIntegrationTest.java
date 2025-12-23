package com.securities.kuku.ledger.resilience;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securities.kuku.ledger.application.port.in.command.DepositCommand;
import com.securities.kuku.ledger.application.port.out.AccountPort;
import com.securities.kuku.ledger.application.port.out.BalancePort;
import com.securities.kuku.ledger.application.port.out.JournalEntryPort;
import com.securities.kuku.ledger.application.port.out.TransactionPort;
import com.securities.kuku.ledger.application.service.DepositService;
import com.securities.kuku.ledger.domain.Account;
import com.securities.kuku.ledger.domain.AccountType;
import com.securities.kuku.ledger.domain.Balance;
import com.securities.kuku.ledger.domain.Transaction;
import com.securities.kuku.ledger.domain.TransactionStatus;
import com.securities.kuku.ledger.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Spring Retry의 @Retryable 동작을 검증하는 통합 테스트. */
@SpringBootTest
@ActiveProfiles("test")
public class RetryIntegrationTest {

  @Autowired private DepositService depositService;

  @MockitoBean private AccountPort accountPort;
  @MockitoBean private BalancePort balancePort;
  @MockitoBean private TransactionPort transactionPort;
  @MockitoBean private JournalEntryPort journalEntryPort;
  @MockitoBean private Clock clock;

  @Test
  @DisplayName("OptimisticLock 발생 시 3회까지 재시도하고 성공하면 정상 처리된다")
  void shouldRetryAndSucceed() {
    // Given
    Long accountId = 1L;
    Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");
    DepositCommand command =
        new DepositCommand(accountId, BigDecimal.TEN, "Test deposit", "ref-retry-001");

    Account mockAccount =
        new Account(accountId, 1L, "ACC-001", "KRW", AccountType.USER_SECURITIES, fixedTime);
    Balance mockBalance =
        new Balance(accountId, BigDecimal.ZERO, BigDecimal.ZERO, 1L, null, fixedTime);

    when(transactionPort.findByBusinessRefId("ref-retry-001")).thenReturn(Optional.empty());
    when(accountPort.findById(accountId)).thenReturn(Optional.of(mockAccount));
    when(balancePort.findByAccountId(accountId)).thenReturn(Optional.of(mockBalance));
    when(clock.instant()).thenReturn(fixedTime);

    Transaction savedTx =
        new Transaction(
            1L,
            TransactionType.DEPOSIT,
            "Test deposit",
            "ref-retry-001",
            TransactionStatus.POSTED,
            null,
            fixedTime);

    when(transactionPort.save(any(Transaction.class)))
        .thenThrow(
            new ObjectOptimisticLockingFailureException(
                "Optimistic lock failed", new Throwable())) // 1st attempt
        .thenThrow(
            new ObjectOptimisticLockingFailureException(
                "Optimistic lock failed", new Throwable())) // 2nd attempt (1st retry)
        .thenReturn(savedTx); // 3rd attempt (2nd retry) -> Success

    // When
    depositService.deposit(command);

    // Then
    verify(transactionPort, times(3)).save(any(Transaction.class));
  }
}
