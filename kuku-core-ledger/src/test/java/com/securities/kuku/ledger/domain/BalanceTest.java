package com.securities.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceTest {

    @Test
    @DisplayName("잔고 생성 시 필수 정보가 없으면 예외가 발생한다")
    void createBalance_validation() {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        assertThatThrownBy(() -> new Balance(null, BigDecimal.ZERO, 1L, 1L, fixedTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account ID");

        assertThatThrownBy(() -> new Balance(1L, null, 1L, 1L, fixedTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount");
    }

    @Test
    @DisplayName("정상적인 잔고 생성")
    void createBalance_success() {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        Balance balance = new Balance(1L, BigDecimal.valueOf(5000), 1L, 100L, fixedTime);

        assertThat(balance.getAccountId()).isEqualTo(1L);
        assertThat(balance.getAmount()).isEqualTo(BigDecimal.valueOf(5000));
        assertThat(balance.getUpdatedAt()).isEqualTo(fixedTime);
    }
}
