package com.securities.kuku.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStatusTest {

  @Nested
  @DisplayName("canTransitionTo() 메서드")
  class CanTransitionTo {

    @ParameterizedTest
    @CsvSource({
      "CREATED, VALIDATED, true",
      "CREATED, REJECTED, true",
      "CREATED, FILLED, false",
      "CREATED, CANCELLED, false",
      "VALIDATED, FILLED, true",
      "VALIDATED, REJECTED, true",
      "VALIDATED, CANCELLED, true",
      "VALIDATED, CREATED, false",
      "FILLED, CREATED, false",
      "FILLED, VALIDATED, false",
      "FILLED, REJECTED, false",
      "FILLED, CANCELLED, false",
      "REJECTED, CREATED, false",
      "REJECTED, VALIDATED, false",
      "REJECTED, FILLED, false",
      "REJECTED, CANCELLED, false",
      "CANCELLED, CREATED, false",
      "CANCELLED, VALIDATED, false",
      "CANCELLED, FILLED, false",
      "CANCELLED, REJECTED, false"
    })
    @DisplayName("상태 전이 규칙 검증")
    void verifyTransitionRules(OrderStatus from, OrderStatus to, boolean expected) {
      assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    @DisplayName("CREATED에서 VALIDATED 또는 REJECTED로만 전이 가능")
    void created_canTransitionToValidatedOrRejected() {
      assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.VALIDATED)).isTrue();
      assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.REJECTED)).isTrue();
      assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.FILLED)).isFalse();
      assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("VALIDATED에서 FILLED, REJECTED 또는 CANCELLED로 전이 가능")
    void validated_canTransitionToFilledRejectedOrCancelled() {
      assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.FILLED)).isTrue();
      assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.REJECTED)).isTrue();
      assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
      assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.CREATED)).isFalse();
    }
  }

  @Nested
  @DisplayName("isTerminal() 메서드")
  class IsTerminal {

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"FILLED", "REJECTED", "CANCELLED"})
    @DisplayName("종료 상태 확인")
    void terminalStates(OrderStatus status) {
      assertThat(status.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"CREATED", "VALIDATED"})
    @DisplayName("비종료 상태 확인")
    void nonTerminalStates(OrderStatus status) {
      assertThat(status.isTerminal()).isFalse();
    }
  }
}
