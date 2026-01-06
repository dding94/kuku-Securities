package com.securities.kuku.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderStatus;
import com.securities.kuku.order.domain.OrderType;
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

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-06T02:00:00Z");

  @Mock private OrderPort orderPort;

  private GetOrderService getOrderService;

  @BeforeEach
  void setUp() {
    getOrderService = new GetOrderService(orderPort);
  }

  @Nested
  @DisplayName("getOrder")
  class GetOrder {

    @Test
    @DisplayName("존재하는 주문 ID로 조회 시 주문을 반환한다")
    void success_returnsOrder_whenOrderExists() {
      // Given
      Long orderId = 1L;
      Order order =
          new Order(
              orderId,
              1L,
              "AAPL",
              new BigDecimal("10"),
              OrderSide.BUY,
              OrderType.MARKET,
              new BigDecimal("150.00"),
              OrderStatus.VALIDATED,
              null,
              "ref-001",
              null,
              null,
              FIXED_TIME,
              FIXED_TIME);
      given(orderPort.findById(orderId)).willReturn(Optional.of(order));

      // When
      Optional<Order> result = getOrderService.getOrder(orderId);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(orderId);
      assertThat(result.get().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 조회 시 빈 Optional을 반환한다")
    void success_returnsEmpty_whenOrderNotExists() {
      // Given
      Long orderId = 999L;
      given(orderPort.findById(orderId)).willReturn(Optional.empty());

      // When
      Optional<Order> result = getOrderService.getOrder(orderId);

      // Then
      assertThat(result).isEmpty();
    }
  }
}
