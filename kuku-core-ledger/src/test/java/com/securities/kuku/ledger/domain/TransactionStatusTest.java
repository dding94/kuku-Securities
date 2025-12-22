package com.securities.kuku.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionStatusTest {

  @Test
  @DisplayName("TransactionStatus는 PENDING, POSTED, REVERSED, UNKNOWN 상태를 가진다")
  void transactionStatus_hasFourStates() {
    assertThat(TransactionStatus.values()).hasSize(4);
    assertThat(TransactionStatus.valueOf("PENDING")).isEqualTo(TransactionStatus.PENDING);
    assertThat(TransactionStatus.valueOf("POSTED")).isEqualTo(TransactionStatus.POSTED);
    assertThat(TransactionStatus.valueOf("REVERSED")).isEqualTo(TransactionStatus.REVERSED);
    assertThat(TransactionStatus.valueOf("UNKNOWN")).isEqualTo(TransactionStatus.UNKNOWN);
  }

  @Test
  @DisplayName("PENDING 상태는 아직 확정되지 않은 트랜잭션이다")
  void pending_isNotConfirmed() {
    assertThat(TransactionStatus.PENDING.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("POSTED 상태는 확정된 트랜잭션이다")
  void posted_isConfirmed() {
    assertThat(TransactionStatus.POSTED.isConfirmed()).isTrue();
  }

  @Test
  @DisplayName("REVERSED 상태는 확정되지 않은(무효화된) 트랜잭션이다")
  void reversed_isNotConfirmed() {
    assertThat(TransactionStatus.REVERSED.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("POSTED 상태만 역분개 가능하다")
  void onlyPosted_canBeReversed() {
    assertThat(TransactionStatus.PENDING.canBeReversed()).isFalse();
    assertThat(TransactionStatus.POSTED.canBeReversed()).isTrue();
    assertThat(TransactionStatus.REVERSED.canBeReversed()).isFalse();
    assertThat(TransactionStatus.UNKNOWN.canBeReversed()).isFalse();
  }

  @Test
  @DisplayName("UNKNOWN 상태는 확정된 트랜잭션이 아니다")
  void unknown_isNotConfirmed() {
    assertThat(TransactionStatus.UNKNOWN.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("PENDING에서 UNKNOWN으로 전환 가능하다")
  void pending_canTransitionToUnknown() {
    assertThat(TransactionStatus.PENDING.canTransitionTo(TransactionStatus.UNKNOWN)).isTrue();
  }

  @Test
  @DisplayName("UNKNOWN에서 POSTED로 전환 가능하다")
  void unknown_canTransitionToPosted() {
    assertThat(TransactionStatus.UNKNOWN.canTransitionTo(TransactionStatus.POSTED)).isTrue();
  }

  @Test
  @DisplayName("UNKNOWN에서 REVERSED로 직접 전환 불가하다")
  void unknown_cannotTransitionToReversed() {
    assertThat(TransactionStatus.UNKNOWN.canTransitionTo(TransactionStatus.REVERSED)).isFalse();
  }

  @Test
  @DisplayName("POSTED에서 REVERSED로 전환 가능하다")
  void posted_canTransitionToReversed() {
    assertThat(TransactionStatus.POSTED.canTransitionTo(TransactionStatus.REVERSED)).isTrue();
  }

  @Test
  @DisplayName("REVERSED는 최종 상태로 다른 상태로 전환 불가하다")
  void reversed_cannotTransitionToAny() {
    assertThat(TransactionStatus.REVERSED.canTransitionTo(TransactionStatus.PENDING)).isFalse();
    assertThat(TransactionStatus.REVERSED.canTransitionTo(TransactionStatus.POSTED)).isFalse();
    assertThat(TransactionStatus.REVERSED.canTransitionTo(TransactionStatus.UNKNOWN)).isFalse();
  }
}
