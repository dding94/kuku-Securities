package com.securities.kuku.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BalanceTest {

  @Test
  @DisplayName("계좌 ID가 없으면 예외가 발생한다")
  void createBalance_throwsException_whenAccountIdIsNull() {
    Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
    assertThatThrownBy(() -> new Balance(null, BigDecimal.ZERO, BigDecimal.ZERO, 1L, 1L, fixedTime))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Account ID");
  }

  @Test
  @DisplayName("금액이 없으면 예외가 발생한다")
  void createBalance_throwsException_whenAmountIsNull() {
    Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
    assertThatThrownBy(() -> new Balance(1L, null, BigDecimal.ZERO, 1L, 1L, fixedTime))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Amount");
  }

  @Test
  @DisplayName("홀드 금액이 없으면 예외가 발생한다")
  void createBalance_throwsException_whenHoldAmountIsNull() {
    Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
    assertThatThrownBy(() -> new Balance(1L, BigDecimal.ZERO, null, 1L, 1L, fixedTime))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("HoldAmount");
  }

  @Test
  @DisplayName("홀드 금액이 음수이면 예외가 발생한다")
  void createBalance_throwsException_whenHoldAmountIsNegative() {
    Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
    assertThatThrownBy(
            () -> new Balance(1L, BigDecimal.ZERO, BigDecimal.valueOf(-100), 1L, 1L, fixedTime))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("HoldAmount");
  }

  @Test
  @DisplayName("정상적인 잔고 생성 및 가용 금액 확인")
  void createBalance_success() {
    Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
    Balance balance =
        new Balance(1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(1000), 1L, 100L, fixedTime);

    assertThat(balance.getAccountId()).isEqualTo(1L);
    assertThat(balance.getAmount()).isEqualTo(BigDecimal.valueOf(5000));
    assertThat(balance.getHoldAmount()).isEqualTo(BigDecimal.valueOf(1000));
    assertThat(balance.getAvailableAmount()).isEqualTo(BigDecimal.valueOf(4000));
    assertThat(balance.getUpdatedAt()).isEqualTo(fixedTime);
  }

  @Test
  @DisplayName("입금 시 잔액이 증가하고 업데이트 일시가 갱신되어야 한다")
  void deposit_ShouldIncreaseAmountAndUpdateTimestamp() {
    // Given
    Long accountId = 1L;
    BigDecimal initialAmount = new BigDecimal("1000");
    Instant initialTime = Instant.parse("2025-01-01T03:00:00Z");
    Balance balance = new Balance(accountId, initialAmount, BigDecimal.ZERO, 1L, 100L, initialTime);

    BigDecimal depositAmount = new BigDecimal("500");
    Long transactionId = 200L;
    Instant now = Instant.parse("2025-12-25T01:00:00Z");

    Balance updatedBalance = balance.deposit(depositAmount, transactionId, now);

    // Then
    assertThat(updatedBalance.getAmount()).isEqualByComparingTo(new BigDecimal("1500"));
    assertThat(updatedBalance.getLastTransactionId()).isEqualTo(transactionId);
    assertThat(updatedBalance.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("가용 금액은 총 금액에서 홀드 금액을 뺀 값이어야 한다")
  void getAvailableAmount_ShouldReturnCorrectValue() {
    // Given
    Long accountId = 1L;
    BigDecimal amount = new BigDecimal("1000");
    BigDecimal holdAmount = new BigDecimal("300");
    Instant fixedTime = Instant.parse("2025-01-01T03:00:00Z");
    Balance balance = new Balance(accountId, amount, holdAmount, 1L, 100L, fixedTime);

    // When
    BigDecimal availableAmount = balance.getAvailableAmount();

    // Then
    assertThat(availableAmount).isEqualByComparingTo(new BigDecimal("700"));
  }
}
