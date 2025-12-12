package com.securities.kuku.ledger.test.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.securities.kuku.ledger.adapter.out.persistence.entity.AccountJpaEntity;
import com.securities.kuku.ledger.adapter.out.persistence.entity.BalanceJpaEntity;
import com.securities.kuku.ledger.domain.AccountType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ledger 도메인 테스트를 위한 Fixture 및 Helper 클래스.
 * 테스트 데이터 생성, 정리(CleanUp), 정합성 검증을 담당합니다.
 * <p>
 * 개선사항:
 * - testRunId를 통한 테스트 격리
 * - SUM(amount) 기반의 재무적 정합성 검증
 * - CleanUp 로깅
 * <p>
 * 주의: @Component가 아닌 @Import로 빈을 등록합니다. (테스트 전용 클래스)
 */
public class LedgerTestFixture {

    private static final Logger log = LoggerFactory.getLogger(LedgerTestFixture.class);

    private final EntityManager entityManager;
    private final String testRunId;

    public LedgerTestFixture(EntityManager entityManager) {
        this.entityManager = entityManager;
        // 테스트 실행 단위별 고유 ID 생성 (8자리 UUID)
        this.testRunId = UUID.randomUUID().toString().substring(0, 8);
    }

    public String generateBusinessRefId(Long accountId, long threadId) {
        // ID 충돌 방지를 위해 testRunId 포함
        return String.format("Tx-%s-%d-%d", testRunId, accountId, threadId);
    }

    /**
     * 테스트용 계좌와 잔액을 생성합니다.
     */
    @Transactional
    public Long createAccountWithBalance(BigDecimal amount) {
        // Long.MIN_VALUE 방어: Math.abs(Long.MIN_VALUE) == Long.MIN_VALUE (여전히 음수)
        // 비트마스크로 MSB를 0으로 만들어 항상 양수 보장
        Long accountId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

        AccountJpaEntity account = new AccountJpaEntity(
                accountId, 100L, "ACC-" + accountId, "KRW", AccountType.USER_CASH, Instant.now());
        entityManager.persist(account);

        BalanceJpaEntity balance = new BalanceJpaEntity(
                accountId, amount, BigDecimal.ZERO, null, null, Instant.now());
        entityManager.persist(balance);

        entityManager.flush();
        entityManager.clear();
        return accountId;
    }

    /**
     * 해당 계좌와 관련된 모든 데이터를 정리합니다.
     * EntityManager를 사용하여 직접 JPQL을 실행하므로 Repository 인터페이스를 오염시키지 않습니다.
     */
    @Transactional
    public void cleanup(Long accountId) {
        if (accountId == null)
            return;

        // cleanup 범위 한정: 현재 testRunId에 해당하는 데이터만 삭제
        String refPattern = String.format("Tx-%s-%d-%%", testRunId, accountId);

        int deletedJournalEntries = entityManager
                .createQuery("DELETE FROM JournalEntryJpaEntity e WHERE e.accountId = :accountId")
                .setParameter("accountId", accountId)
                .executeUpdate();

        int deletedTransactions = entityManager
                .createQuery("DELETE FROM TransactionJpaEntity t WHERE t.businessRefId LIKE :refPattern")
                .setParameter("refPattern", refPattern)
                .executeUpdate();

        int deletedBalances = entityManager.createQuery("DELETE FROM BalanceJpaEntity b WHERE b.accountId = :accountId")
                .setParameter("accountId", accountId)
                .executeUpdate();

        int deletedAccounts = entityManager.createQuery("DELETE FROM AccountJpaEntity a WHERE a.id = :accountId")
                .setParameter("accountId", accountId)
                .executeUpdate();

        log.info("Cleanup for account {}: Deleted {} Journals, {} Tx, {} Balances, {} Accounts",
                accountId, deletedJournalEntries, deletedTransactions, deletedBalances, deletedAccounts);
    }

    public void assertBalance(Long accountId, BigDecimal expectedAmount) {
        BalanceJpaEntity balance = entityManager.find(BalanceJpaEntity.class, accountId);
        assertThat(balance).isNotNull();
        assertThat(balance.getAmount()).isEqualByComparingTo(expectedAmount);
    }

    /**
     * 원장 정합성 검증: Transaction 개수, JournalEntry 개수, 총액 검증.
     *
     * @param accountId            검증 대상 계좌 ID
     * @param expectedSuccessCount 성공한 트랜잭션 수
     * @param amountPerTx          건당 금액 (항상 양수, DEBIT/CREDIT 구분 없이 절대값)
     */
    public void assertLedgerConsistency(Long accountId, int expectedSuccessCount, BigDecimal amountPerTx) {
        String refPattern = String.format("Tx-%s-%d-%%", testRunId, accountId);

        // 1. Transaction 개수 검증
        Long txCount = entityManager.createQuery(
                "SELECT COUNT(t) FROM TransactionJpaEntity t WHERE t.businessRefId LIKE :refPattern", Long.class)
                .setParameter("refPattern", refPattern)
                .getSingleResult();

        assertThat(txCount).as("Transaction count should match success count")
                .isEqualTo(expectedSuccessCount);

        // 2. JournalEntry 개수 검증 (Debit/Credit 여부에 따라 달라질 수 있으나, 현재 로직은 1 Tx = 1 Entry
        // 가정)
        // 만약 1 Tx = 2 Entry라면 expectedSuccessCount * 2 가 되어야 함.
        // 현재 비즈니스 로직(WithdrawService)은 1개의 JournalEntry만 생성함 (Debit).
        Long journalCount = entityManager.createQuery(
                "SELECT COUNT(e) FROM JournalEntryJpaEntity e WHERE e.accountId = :accountId", Long.class)
                .setParameter("accountId", accountId)
                .getSingleResult();

        assertThat(journalCount).as("JournalEntry count should match success count")
                .isEqualTo(expectedSuccessCount);

        // 3. JournalEntry 총액 검증 (Financial Integrity)
        // 성공 횟수 * 건당 금액 = DB에 기록된 총 금액 합계
        BigDecimal expectedTotalAmount = amountPerTx.multiply(new BigDecimal(expectedSuccessCount));

        BigDecimal actualTotalAmount = entityManager.createQuery(
                "SELECT COALESCE(SUM(e.amount), 0) FROM JournalEntryJpaEntity e WHERE e.accountId = :accountId",
                BigDecimal.class)
                .setParameter("accountId", accountId)
                .getSingleResult();

        assertThat(actualTotalAmount).as("Total Journal amount should match verification")
                .isEqualByComparingTo(expectedTotalAmount);
    }

    /**
     * 해당 계좌의 DEBIT 합계가 기대값과 일치하는지 검증합니다.
     * 출금(Withdraw) 테스트에서 사용 - 출금은 DEBIT으로 기록됩니다.
     *
     * @param accountId     검증 대상 계좌 ID
     * @param expectedTotal 기대되는 DEBIT 합계 (항상 양수)
     */
    public void assertDebitTotal(Long accountId, BigDecimal expectedTotal) {
        BigDecimal debitSum = entityManager.createQuery(
                "SELECT COALESCE(SUM(e.amount), 0) FROM JournalEntryJpaEntity e " +
                        "WHERE e.accountId = :accountId AND e.entryType = 'DEBIT'",
                BigDecimal.class)
                .setParameter("accountId", accountId)
                .getSingleResult();

        assertThat(debitSum)
                .as("DEBIT total for account %d should match expected", accountId)
                .isEqualByComparingTo(expectedTotal);
    }

    /**
     * 해당 계좌의 CREDIT 합계가 기대값과 일치하는지 검증합니다.
     * 입금(Deposit) 테스트에서 사용 - 입금은 CREDIT으로 기록됩니다.
     *
     * @param accountId     검증 대상 계좌 ID
     * @param expectedTotal 기대되는 CREDIT 합계 (항상 양수)
     */
    public void assertCreditTotal(Long accountId, BigDecimal expectedTotal) {
        BigDecimal creditSum = entityManager.createQuery(
                "SELECT COALESCE(SUM(e.amount), 0) FROM JournalEntryJpaEntity e " +
                        "WHERE e.accountId = :accountId AND e.entryType = 'CREDIT'",
                BigDecimal.class)
                .setParameter("accountId", accountId)
                .getSingleResult();

        assertThat(creditSum)
                .as("CREDIT total for account %d should match expected", accountId)
                .isEqualByComparingTo(expectedTotal);
    }
}
