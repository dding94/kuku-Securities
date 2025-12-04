package com.securities.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStatusTest {

    @Test
    @DisplayName("TransactionStatus는 PENDING, POSTED, REVERSED 상태를 가진다")
    void transactionStatus_hasThreeStates() {
        assertThat(TransactionStatus.values()).hasSize(3);
        assertThat(TransactionStatus.valueOf("PENDING")).isEqualTo(TransactionStatus.PENDING);
        assertThat(TransactionStatus.valueOf("POSTED")).isEqualTo(TransactionStatus.POSTED);
        assertThat(TransactionStatus.valueOf("REVERSED")).isEqualTo(TransactionStatus.REVERSED);
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
    }
}
