package com.securities.kuku.ledger.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.securities.kuku.ledger.application.port.in.command.DepositCommand;
import com.securities.kuku.ledger.application.port.in.command.WithdrawCommand;
import com.securities.kuku.ledger.application.service.DepositService;
import com.securities.kuku.ledger.application.service.WithdrawService;
import com.securities.kuku.ledger.domain.InsufficientBalanceException;
import com.securities.kuku.ledger.test.support.ConcurrencyRunner;
import com.securities.kuku.ledger.test.support.ConcurrencyRunner.ExecutionResult;
import com.securities.kuku.ledger.test.support.LedgerTestFixture;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Ledger 동시성 테스트.
 * 동일 계좌에 대한 동시 입출금 시 데이터 정합성을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(LedgerTestFixture.class)
class LedgerConcurrencyTest {

    @Autowired
    private DepositService depositService;

    @Autowired
    private WithdrawService withdrawService;

    @Autowired
    private LedgerTestFixture fixture;

    private Long accountId;

    @AfterEach
    void tearDown() {
        fixture.cleanup(accountId);
    }

    @Nested
    @DisplayName("동시 입금 테스트")
    class ConcurrentDepositTest {

        @Test
        @DisplayName("10개 스레드가 동시에 100원씩 입금하면 데이터 정합성이 유지되어야 한다")
        void 동시_입금시_정합성_유지() {
            // Given: 잔액 0원 계좌 생성
            BigDecimal initialBalance = BigDecimal.ZERO;
            BigDecimal depositAmount = new BigDecimal("100");
            int threadCount = 10;
            accountId = fixture.createAccountWithBalance(initialBalance);

            // When: 10개 스레드가 동시 입금
            ExecutionResult result = ConcurrencyRunner.run(threadCount, () -> {
                long threadId = Thread.currentThread().threadId();
                String businessRefId = fixture.generateBusinessRefId(accountId, threadId);

                DepositCommand command = DepositCommand.of(
                        accountId, depositAmount, "Concurrent deposit", businessRefId);
                depositService.deposit(command);
            }, ObjectOptimisticLockingFailureException.class);

            // Then: 결과 검증
            result.assertNoUnexpectedExceptions();
            assertThat(result.getSuccessCount() + result.getExpectedFailureCount())
                    .as("All threads should complete (success or expected failure)")
                    .isEqualTo(threadCount);

            // 성공 횟수만큼 잔액이 증가했는지 확인 (핵심 정합성 검증)
            BigDecimal expectedBalance = depositAmount.multiply(new BigDecimal(result.getSuccessCount()));
            fixture.assertBalance(accountId, expectedBalance);

            // 원장 정합성 검증: CREDIT 합계 = 성공 횟수 × 100
            fixture.assertCreditTotal(accountId, expectedBalance);
        }
    }

    @Nested
    @DisplayName("동시 입출금 혼합 테스트")
    class ConcurrentMixedTest {

        @Test
        @DisplayName("5개 입금 + 5개 출금 동시 실행 시 데이터 정합성이 유지되어야 한다")
        void 동시_입출금_혼합시_정합성_유지() {
            // Given: 잔액 5000원 계좌 생성
            BigDecimal initialBalance = new BigDecimal("5000");
            BigDecimal txAmount = new BigDecimal("100");
            int depositThreads = 5;
            int withdrawThreads = 5;
            accountId = fixture.createAccountWithBalance(initialBalance);

            // 작업 분배를 위한 인덱스 (threadId 대신 결정론적 분배)
            AtomicInteger taskIndex = new AtomicInteger(0);

            // When: 혼합 스레드 실행
            ExecutionResult result = ConcurrencyRunner.run(depositThreads + withdrawThreads, () -> {
                int index = taskIndex.getAndIncrement();
                long threadId = Thread.currentThread().threadId();
                String businessRefId = fixture.generateBusinessRefId(accountId, threadId);

                // 첫 5개는 입금, 나머지 5개는 출금 (결정론적 분배)
                if (index < depositThreads) {
                    DepositCommand command = DepositCommand.of(
                            accountId, txAmount, "Mixed deposit", businessRefId);
                    depositService.deposit(command);
                } else {
                    WithdrawCommand command = WithdrawCommand.of(
                            accountId, txAmount, "Mixed withdraw", businessRefId);
                    withdrawService.withdraw(command);
                }
            }, ObjectOptimisticLockingFailureException.class, InsufficientBalanceException.class);

            // Then: 결과 검증
            result.assertNoUnexpectedExceptions();

            // 최종 잔액 = 초기 잔액 + (CREDIT 합계) - (DEBIT 합계)
            // 원장에 기록된 금액으로 정합성 검증
            BigDecimal creditTotal = fixture.getCreditTotal(accountId);
            BigDecimal debitTotal = fixture.getDebitTotal(accountId);
            BigDecimal expectedBalance = initialBalance.add(creditTotal).subtract(debitTotal);

            fixture.assertBalance(accountId, expectedBalance);
        }
    }

    @Nested
    @DisplayName("Lost Update 방지 테스트")
    class LostUpdatePreventionTest {

        @Test
        @DisplayName("20개 스레드가 각각 1원씩 출금할 때 모든 성공한 업데이트가 반영되어야 한다")
        void 연속_업데이트_모두_반영() {
            // Given: 잔액 10000원 계좌 생성
            BigDecimal initialBalance = new BigDecimal("10000");
            BigDecimal withdrawAmount = new BigDecimal("1");
            int threadCount = 20;
            accountId = fixture.createAccountWithBalance(initialBalance);

            // When: 20개 스레드가 동시에 1원씩 출금
            ExecutionResult result = ConcurrencyRunner.run(threadCount, () -> {
                long threadId = Thread.currentThread().threadId();
                String businessRefId = fixture.generateBusinessRefId(accountId, threadId);

                WithdrawCommand command = WithdrawCommand.of(
                        accountId, withdrawAmount, "Lost update test", businessRefId);
                withdrawService.withdraw(command);
            }, ObjectOptimisticLockingFailureException.class, InsufficientBalanceException.class);

            // Then: 결과 검증
            result.assertNoUnexpectedExceptions();
            int successCount = result.getSuccessCount();

            // Lost Update가 없다면:
            // 최종 잔액 = 초기 잔액 - (성공 횟수 × 1원)
            BigDecimal expectedBalance = initialBalance.subtract(
                    withdrawAmount.multiply(new BigDecimal(successCount)));
            fixture.assertBalance(accountId, expectedBalance);

            // JournalEntry DEBIT 합계 = 성공 횟수 × 1원
            BigDecimal expectedDebitTotal = withdrawAmount.multiply(new BigDecimal(successCount));
            fixture.assertDebitTotal(accountId, expectedDebitTotal);

            // 원장 정합성 전체 검증
            fixture.assertLedgerConsistency(accountId, successCount, withdrawAmount);
        }
    }
}
