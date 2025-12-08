package com.securities.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

        @Test
        @DisplayName("금액이 0 이하이면 예외가 발생한다")
        void createJournalEntry_throwsException_whenAmountIsNotPositive() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                assertThatThrownBy(() -> new JournalEntry(1L, 1L, 1L, BigDecimal.ZERO, JournalEntry.EntryType.DEBIT,
                                fixedTime))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("생성 일시가 없으면 예외가 발생한다")
        void createJournalEntry_throwsException_whenCreatedAtIsNull() {
                assertThatThrownBy(
                                () -> new JournalEntry(1L, 1L, 1L, BigDecimal.TEN, JournalEntry.EntryType.DEBIT, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("CreatedAt");
        }

        @Test
        @DisplayName("정상적인 분개 생성")
        void createJournalEntry_success() {
                Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
                JournalEntry entry = new JournalEntry(1L, 10L, 100L, BigDecimal.valueOf(1000),
                                JournalEntry.EntryType.CREDIT,
                                fixedTime);

                assertThat(entry.getId()).isEqualTo(1L);
                assertThat(entry.getTransactionId()).isEqualTo(10L);
                assertThat(entry.getAccountId()).isEqualTo(100L);
                assertThat(entry.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
                assertThat(entry.getEntryType()).isEqualTo(JournalEntry.EntryType.CREDIT);
                assertThat(entry.getCreatedAt()).isEqualTo(fixedTime);
        }
}
