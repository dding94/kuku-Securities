package com.securities.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    @DisplayName("트랜잭션 생성 시 필수 정보가 없으면 예외가 발생한다")
    void createTransaction_validation() {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        assertThatThrownBy(() -> new Transaction(null, "DEPOSIT", "Test", fixedTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID");

        assertThatThrownBy(() -> new Transaction(1L, null, "Test", fixedTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type");

        assertThatThrownBy(() -> new Transaction(1L, "DEPOSIT", "Test", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CreatedAt");
    }

    @Test
    @DisplayName("정상적인 트랜잭션 생성")
    void createTransaction_success() {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        Transaction transaction = new Transaction(1L, "DEPOSIT", "Test Description", fixedTime);

        assertThat(transaction.getId()).isEqualTo(1L);
        assertThat(transaction.getType()).isEqualTo("DEPOSIT");
        assertThat(transaction.getDescription()).isEqualTo("Test Description");
        assertThat(transaction.getCreatedAt()).isEqualTo(fixedTime);
    }
}
