package com.securities.kuku.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderStatus;
import com.securities.kuku.order.domain.OrderType;
import com.securities.kuku.order.domain.exception.InvalidOrderStateException;
import com.securities.kuku.order.domain.exception.OrderNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-06T02:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneId.of("Asia/Seoul"));

  @Mock private OrderPort orderPort;

  private CancelOrderService cancelOrderService;

  @BeforeEach
  void setUp() {
    cancelOrderService = new CancelOrderService(FIXED_CLOCK, orderPort);
  }

  private Order createOrder(Long id, OrderStatus status) {
    return new Order(
        id,
        1L,
        "AAPL",
        new BigDecimal("10"),
        OrderSide.BUY,
        OrderType.MARKET,
        new BigDecimal("150.00"),
        status,
        null,
        "ref-001",
        null,
        null,
        FIXED_TIME,
        FIXED_TIME);
  }

  @Nested
  @DisplayName("cancelOrder")
  class CancelOrder {

    @Test
    @DisplayName("VALIDATED 상태의 주문을 취소하면 CANCELLED 상태로 변경된다")
    void success_cancelValidatedOrder() {
      // Given
      Long orderId = 1L;
      Order order = createOrder(orderId, OrderStatus.VALIDATED);
      given(orderPort.findById(orderId)).willReturn(Optional.of(order));
      given(orderPort.update(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

      // When
      Order result = cancelOrderService.cancelOrder(orderId);

      // Then
      assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("존재하지 않는 주문 취소 시 OrderNotFoundException을 던진다")
    void failure_throwsOrderNotFoundException_whenOrderNotExists() {
      // Given
      Long orderId = 999L;
      given(orderPort.findById(orderId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> cancelOrderService.cancelOrder(orderId))
          .isInstanceOf(OrderNotFoundException.class)
          .hasMessageContaining("999");
    }

    @Test
    @DisplayName("이미 체결된 주문 취소 시 InvalidOrderStateException을 던진다")
    void failure_throwsInvalidOrderStateException_whenOrderIsFilled() {
      // Given
      Long orderId = 1L;
      Order filledOrder = createOrder(orderId, OrderStatus.FILLED);
      given(orderPort.findById(orderId)).willReturn(Optional.of(filledOrder));

      // When & Then
      assertThatThrownBy(() -> cancelOrderService.cancelOrder(orderId))
          .isInstanceOf(InvalidOrderStateException.class);
    }
  }
}
