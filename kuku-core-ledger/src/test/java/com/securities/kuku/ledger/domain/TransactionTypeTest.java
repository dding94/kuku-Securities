package com.securities.kuku.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TransactionTypeTest {

    private static final Instant FIXED_TIME = Instant.parse("2025-01-01T03:00:00Z");
    private static final Long ACCOUNT_ID = 100L;
    private static final Long TX_ID = 1L;

    @Nested
    @DisplayName("applyTo() 메서드")
    class ApplyTo {

        @Test
        @DisplayName("DEPOSIT은 잔액을 증가시킨다")
        void deposit_increasesBalance() {
            Balance balance = new Balance(ACCOUNT_ID, new BigDecimal("1000"), BigDecimal.ZERO, 0L, null, FIXED_TIME);
            BigDecimal amount = new BigDecimal("500");

            Balance newBalance = TransactionType.DEPOSIT.applyTo(balance, amount, TX_ID, FIXED_TIME);

            assertThat(newBalance.getAmount()).isEqualByComparingTo(new BigDecimal("1500"));
        }

        @Test
        @DisplayName("WITHDRAWAL은 잔액을 감소시킨다")
        void withdrawal_decreasesBalance() {
            Balance balance = new Balance(ACCOUNT_ID, new BigDecimal("1000"), BigDecimal.ZERO, 0L, null, FIXED_TIME);
            BigDecimal amount = new BigDecimal("300");

            Balance newBalance = TransactionType.WITHDRAWAL.applyTo(balance, amount, TX_ID, FIXED_TIME);

            assertThat(newBalance.getAmount()).isEqualByComparingTo(new BigDecimal("700"));
        }

        @Test
        @DisplayName("지원하지 않는 타입은 예외를 발생시킨다")
        void unsupportedType_throwsException() {
            Balance balance = new Balance(ACCOUNT_ID, new BigDecimal("1000"), BigDecimal.ZERO, 0L, null, FIXED_TIME);
            BigDecimal amount = new BigDecimal("100");

            assertThatThrownBy(() -> TransactionType.REVERSAL.applyTo(balance, amount, TX_ID, FIXED_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("REVERSAL");
        }
    }
}
