package com.securities.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

        @Test
        @DisplayName("트랜잭션 타입이 없으면 예외가 발생한다")
        void createTransaction_throwsException_whenTypeIsNull() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                assertThatThrownBy(() -> new Transaction(1L, null, "Test", "REF-001",
                                TransactionStatus.POSTED, null, fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Type");
        }

        @Test
        @DisplayName("생성 일시가 없으면 예외가 발생한다")
        void createTransaction_throwsException_whenCreatedAtIsNull() {
                assertThatThrownBy(() -> new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001",
                                TransactionStatus.POSTED, null, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("CreatedAt");
        }

        @Test
        @DisplayName("정상적인 트랜잭션 생성")
        void createTransaction_success() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                Transaction transaction = new Transaction(1L, TransactionType.DEPOSIT, "Test Description", "REF-001",
                                TransactionStatus.POSTED, null, fixedTime);

                assertThat(transaction.getId()).isEqualTo(1L);
                assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
                assertThat(transaction.getDescription()).isEqualTo("Test Description");
                assertThat(transaction.getBusinessRefId()).isEqualTo("REF-001");
                assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.POSTED);
                assertThat(transaction.getReversalOfTransactionId()).isNull();
                assertThat(transaction.getCreatedAt()).isEqualTo(fixedTime);
        }

        @Test
        @DisplayName("트랜잭션 상태가 없으면 예외가 발생한다")
        void createTransaction_throwsException_whenStatusIsNull() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                assertThatThrownBy(() -> new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001",
                                null, null, fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Status");
        }

        @Test
        @DisplayName("역분개 트랜잭션 생성")
        void createTransaction_withReversalOfTransactionId() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                Transaction reversal = new Transaction(2L, TransactionType.DEPOSIT, "Reversal", "REF-001-REV",
                                TransactionStatus.POSTED, 1L, fixedTime);

                assertThat(reversal.getReversalOfTransactionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("역분개 트랜잭션은 POSTED 상태여야 한다")
        void createTransaction_throwsException_whenReversalIsNotPosted() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                assertThatThrownBy(() -> new Transaction(2L, TransactionType.DEPOSIT, "Reversal", "REF-001",
                                TransactionStatus.PENDING, 1L, fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Reversal transaction must be in POSTED state");
        }

        @Test
        @DisplayName("이미 취소된 트랜잭션은 역분개 트랜잭션이 될 수 없다")
        void createTransaction_throwsException_whenReversedHasReversalId() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                assertThatThrownBy(() -> new Transaction(2L, TransactionType.DEPOSIT, "Reversal", "REF-001",
                                TransactionStatus.REVERSED, 1L, fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("A REVERSED transaction cannot be a reversal of another transaction");
        }

        @Test
        @DisplayName("toReversed는 상태가 REVERSED로 변경된 새로운 트랜잭션을 반환한다")
        void toReversed_returnsNewTransactionWithReversedStatus() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                Transaction original = new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001",
                                TransactionStatus.POSTED, null, fixedTime);

                Transaction reversed = original.toReversed();

                assertThat(reversed).isNotSameAs(original);
                assertThat(reversed.getId()).isEqualTo(original.getId());
                assertThat(reversed.getStatus()).isEqualTo(TransactionStatus.REVERSED);
                assertThat(reversed.getCreatedAt()).isEqualTo(original.getCreatedAt());
        }

        @Test
        @DisplayName("POSTED 상태가 아닌 트랜잭션은 역분개할 수 없다")
        void toReversed_throwsException_whenStatusIsNotPosted() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                Transaction pending = new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001",
                                TransactionStatus.PENDING, null, fixedTime);

                assertThatThrownBy(() -> pending.toReversed())
                                .isInstanceOf(InvalidTransactionStateException.class)
                                .hasMessageContaining("Cannot reverse a PENDING transaction");
        }

        @Test
        @DisplayName("이미 취소된 트랜잭션은 다시 역분개할 수 없다")
        void toReversed_throwsException_whenStatusIsAlreadyReversed() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                Transaction reversed = new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001",
                                TransactionStatus.REVERSED, null, fixedTime);

                assertThatThrownBy(() -> reversed.toReversed())
                                .isInstanceOf(InvalidTransactionStateException.class)
                                .hasMessageContaining("Transaction is already reversed");
        }
}
