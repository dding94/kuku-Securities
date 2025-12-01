package com.invest.kuku.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    @DisplayName("계좌 생성 시 필수 정보가 없으면 예외가 발생한다")
    void createAccount_validation() {
        assertThatThrownBy(() -> new Account(null, 1L, "1234", "KRW"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID");

        assertThatThrownBy(() -> new Account(1L, null, "1234", "KRW"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UserId");
    }

    @Test
    @DisplayName("정상적인 계좌 생성")
    void createAccount_success() {
        Account account = new Account(1L, 100L, "123-456", "KRW");

        assertThat(account.getId()).isEqualTo(1L);
        assertThat(account.getUserId()).isEqualTo(100L);
        assertThat(account.getAccountNumber()).isEqualTo("123-456");
        assertThat(account.getCurrency()).isEqualTo("KRW");
    }
}
