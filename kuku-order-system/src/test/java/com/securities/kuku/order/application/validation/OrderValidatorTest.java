package com.securities.kuku.order.application.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.securities.kuku.order.application.port.out.BalanceQueryPort;
import com.securities.kuku.order.application.port.out.PositionQueryPort;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderType;
import com.securities.kuku.order.domain.RejectionReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("OrderValidator")
@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-06T01:00:00Z"); // 10:00 KST
  private static final Long ACCOUNT_ID = 1L;
  private static final String SYMBOL = "005930"; // 삼성전자

  @Mock private BalanceQueryPort balanceQueryPort;
  @Mock private PositionQueryPort positionQueryPort;
  @Mock private MarketHoursPolicy marketHoursPolicy;

  private OrderValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OrderValidator(balanceQueryPort, positionQueryPort, marketHoursPolicy);
  }

  private Order createBuyOrder(BigDecimal quantity, BigDecimal price) {
    return Order.create(
        ACCOUNT_ID,
        SYMBOL,
        quantity,
        OrderSide.BUY,
        OrderType.MARKET,
        price,
        "ref-001",
        FIXED_TIME);
  }

  private Order createSellOrder(BigDecimal quantity) {
    return Order.create(
        ACCOUNT_ID,
        SYMBOL,
        quantity,
        OrderSide.SELL,
        OrderType.MARKET,
        null,
        "ref-002",
        FIXED_TIME);
  }

  @Nested
  @DisplayName("예수금 검증 (매수)")
  class BalanceValidation {

    @Test
    @DisplayName("예수금 부족 시 INSUFFICIENT_BALANCE 반환")
    void returnsInsufficientBalance_whenBalanceIsNotEnough() {
      // Given
      Order order = createBuyOrder(BigDecimal.TEN, new BigDecimal("5000")); // 10주 x 5000원 = 50,000원
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);
      given(balanceQueryPort.getAvailableBalance(ACCOUNT_ID))
          .willReturn(Optional.of(new BigDecimal("10000"))); // 10,000원

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).contains(RejectionReason.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("예수금 충분 시 검증 통과")
    void passesValidation_whenBalanceIsSufficient() {
      // Given
      Order order = createBuyOrder(BigDecimal.TEN, new BigDecimal("5000")); // 10주 x 5000원 = 50,000원
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);
      given(balanceQueryPort.getAvailableBalance(ACCOUNT_ID))
          .willReturn(Optional.of(new BigDecimal("100000"))); // 100,000원

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("계좌 잔액이 없을 때(Optional.empty) INSUFFICIENT_BALANCE 반환")
    void returnsInsufficientBalance_whenAccountNotFound() {
      // Given
      Order order = createBuyOrder(BigDecimal.TEN, new BigDecimal("5000"));
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);
      given(balanceQueryPort.getAvailableBalance(ACCOUNT_ID)).willReturn(Optional.empty());

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).contains(RejectionReason.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("매수 주문 시 가격이 null이면 예외 발생")
    void throwsException_whenPriceIsNullForBuyOrder() {
      // Given
      Order order = createBuyOrder(BigDecimal.TEN, null); // price = null
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);

      // When & Then
      assertThatThrownBy(() -> validator.validate(order))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Price is required");
    }
  }

  @Nested
  @DisplayName("보유 수량 검증 (매도)")
  class PositionValidation {

    @Test
    @DisplayName("보유 수량 부족 시 INSUFFICIENT_QUANTITY 반환")
    void returnsInsufficientQuantity_whenPositionIsNotEnough() {
      // Given
      Order order = createSellOrder(BigDecimal.TEN); // 10주 매도
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);
      given(positionQueryPort.getHoldingQuantity(ACCOUNT_ID, SYMBOL))
          .willReturn(new BigDecimal("5")); // 5주 보유

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).contains(RejectionReason.INSUFFICIENT_QUANTITY);
    }

    @Test
    @DisplayName("보유 수량 충분 시 검증 통과")
    void passesValidation_whenPositionIsSufficient() {
      // Given
      Order order = createSellOrder(BigDecimal.TEN); // 10주 매도
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);
      given(positionQueryPort.getHoldingQuantity(ACCOUNT_ID, SYMBOL))
          .willReturn(new BigDecimal("100")); // 100주 보유

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("보유 수량이 0일 때 INSUFFICIENT_QUANTITY 반환")
    void returnsInsufficientQuantity_whenPositionIsZero() {
      // Given
      Order order = createSellOrder(BigDecimal.TEN);
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(true);
      given(positionQueryPort.getHoldingQuantity(ACCOUNT_ID, SYMBOL)).willReturn(BigDecimal.ZERO);

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).contains(RejectionReason.INSUFFICIENT_QUANTITY);
    }
  }

  @Nested
  @DisplayName("장 운영 시간 검증")
  class MarketHoursValidation {

    @Test
    @DisplayName("장 마감 시 MARKET_CLOSED 반환")
    void returnsMarketClosed_whenMarketIsClosed() {
      // Given
      Order order = createBuyOrder(BigDecimal.ONE, new BigDecimal("1000"));
      given(marketHoursPolicy.isMarketOpen(FIXED_TIME)).willReturn(false);

      // When
      Optional<RejectionReason> result = validator.validate(order);

      // Then
      assertThat(result).contains(RejectionReason.MARKET_CLOSED);
    }
  }
}
