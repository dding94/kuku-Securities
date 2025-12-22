package com.securities.kuku.ledger.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;
import com.securities.kuku.ledger.application.service.WithdrawService;
import com.securities.kuku.ledger.domain.InsufficientBalanceException;
import com.securities.kuku.ledger.test.support.ConcurrencyRunner;
import com.securities.kuku.ledger.test.support.ConcurrencyRunner.ExecutionResult;
import com.securities.kuku.ledger.test.support.LedgerTestFixture;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(LedgerTestFixture.class)
class ConcurrentWithdrawIntegrationTest {

  @Autowired private WithdrawService withdrawService;

  @Autowired private LedgerTestFixture fixture;

  private Long accountId;

  @AfterEach
  void tearDown() {
    fixture.cleanup(accountId);
  }

  @Test
  @DisplayName("동시에 2개 스레드가 전체 잔액을 출금하면 하나만 성공해야 한다")
  void 동시_전액_출금시_하나만_성공해야_한다() {
    // Given: 잔액 1000원 계좌 생성
    BigDecimal initialBalance = new BigDecimal("1000");
    BigDecimal withdrawAmount = new BigDecimal("1000");
    accountId = fixture.createAccountWithBalance(initialBalance);

    // When: 2개 스레드가 동시 출금 (하나만 성공 기대)
    ExecutionResult result =
        ConcurrencyRunner.run(
            2,
            () -> {
              long threadId = Thread.currentThread().threadId();
              // Fixture가 제안하는 BusinessRefId 사용
              String businessRefId = fixture.generateBusinessRefId(accountId, threadId);

              WithdrawCommand command =
                  WithdrawCommand.of(
                      accountId, withdrawAmount, "Concurrent withdraw", businessRefId);
              withdrawService.withdraw(command);
            },
            InsufficientBalanceException.class,
            ObjectOptimisticLockingFailureException.class);

    // Then: 결과 검증
    result.assertNoUnexpectedExceptions();
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getExpectedFailureCount()).isEqualTo(1);
    assertThat(result.getSuccessCount() + result.getExpectedFailureCount()).isEqualTo(2);

    fixture.assertBalance(accountId, BigDecimal.ZERO);
    // Transaction 건수 및 총 금액 검증
    fixture.assertLedgerConsistency(accountId, 1, withdrawAmount);
  }

  @Test
  @DisplayName("10개 스레드가 동시에 100원씩 출금하면 데이터 정합성이 유지되어야 한다")
  void 다중_스레드_동시_출금시_데이터_정합성_유지() {
    // Given: 잔액 1000원 계좌 생성
    BigDecimal initialBalance = new BigDecimal("1000");
    BigDecimal withdrawAmount = new BigDecimal("100");
    int threadCount = 10;
    accountId = fixture.createAccountWithBalance(initialBalance);

    // When: 10개 스레드가 동시 출금 (정합성 유지가 핵심)
    ExecutionResult result =
        ConcurrencyRunner.run(
            threadCount,
            () -> {
              long threadId = Thread.currentThread().threadId();
              String businessRefId = fixture.generateBusinessRefId(accountId, threadId);

              WithdrawCommand command =
                  WithdrawCommand.of(accountId, withdrawAmount, "Multi withdraw", businessRefId);
              withdrawService.withdraw(command);
            },
            InsufficientBalanceException.class,
            ObjectOptimisticLockingFailureException.class);

    // Then: 결과 검증
    result.assertNoUnexpectedExceptions();
    assertThat(result.getSuccessCount() + result.getExpectedFailureCount()).isEqualTo(threadCount);

    // 모두 성공 + 실패 합이 스레드 수와 같아야 함 (낙관적 락으로 인해 재시도가 없으면 실패할 수도 있음, 비즈니스 로직에 따라 다름)
    // 이 시나리오에서는 '잔액이 충분'하더라도 '낙관적 락 충돌'로 실패할 수 있음.
    // 따라서 성공 횟수만큼 잔액이 차감되었는지 확인하는 것이 핵심.

    fixture.assertBalance(
        accountId,
        initialBalance.subtract(withdrawAmount.multiply(new BigDecimal(result.getSuccessCount()))));
    // Transaction 건수 및 총 금액 검증
    fixture.assertLedgerConsistency(accountId, result.getSuccessCount(), withdrawAmount);
  }
}
