package com.securities.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

        @Test
        @DisplayName("트랜잭션 ID가 없으면 예외가 발생한다")
        void createTransaction_throwsException_whenIdIsNull() {
                LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
                assertThatThrownBy(() -> new Transaction(null, TransactionType.DEPOSIT, "Test", "REF-001", fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("ID");
        }

        @Test
        @DisplayName("트랜잭션 타입이 없으면 예외가 발생한다")
        void createTransaction_throwsException_whenTypeIsNull() {
                LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
                assertThatThrownBy(() -> new Transaction(1L, null, "Test", "REF-001", fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Type");
        }

        @Test
        @DisplayName("생성 일시가 없으면 예외가 발생한다")
        void createTransaction_throwsException_whenCreatedAtIsNull() {
                assertThatThrownBy(() -> new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001", null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("CreatedAt");
        }

        @Test
        @DisplayName("정상적인 트랜잭션 생성")
        void createTransaction_success() {
                LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
                Transaction transaction = new Transaction(1L, TransactionType.DEPOSIT, "Test Description", "REF-001",
                                fixedTime);

                assertThat(transaction.getId()).isEqualTo(1L);
                assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
                assertThat(transaction.getDescription()).isEqualTo("Test Description");
                assertThat(transaction.getBusinessRefId()).isEqualTo("REF-001");
                assertThat(transaction.getCreatedAt()).isEqualTo(fixedTime);
        }
}
