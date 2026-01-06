package com.securities.kuku.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.securities.kuku.order.application.port.in.command.PlaceOrderCommand;
import com.securities.kuku.order.application.port.out.OrderPort;
import com.securities.kuku.order.application.validation.OrderValidator;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderStatus;
import com.securities.kuku.order.domain.OrderType;
import com.securities.kuku.order.domain.OrderValidationException;
import com.securities.kuku.order.domain.RejectionReason;
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
class PlaceOrderServiceTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-06T02:00:00Z"); // 11:00 KST
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneId.of("Asia/Seoul"));

  @Mock private OrderPort orderPort;
  @Mock private OrderValidator orderValidator;

  private PlaceOrderService placeOrderService;

  @BeforeEach
  void setUp() {
    placeOrderService = new PlaceOrderService(FIXED_CLOCK, orderPort, orderValidator);
  }

  @Nested
  @DisplayName("placeOrder")
  class PlaceOrder {

    @Test
    @DisplayName("검증 통과 시 VALIDATED 상태의 주문을 저장하고 반환한다")
    void success_savesValidatedOrder() {
      // Given
      PlaceOrderCommand command =
          PlaceOrderCommand.of(
              1L,
              "AAPL",
              new BigDecimal("10"),
              OrderSide.BUY,
              OrderType.MARKET,
              new BigDecimal("150.00"),
              "ref-001");
      given(orderValidator.validate(any(Order.class))).willReturn(Optional.empty());
      given(orderPort.save(any(Order.class)))
          .willAnswer(
              invocation -> {
                Order order = invocation.getArgument(0);
                return new Order(
                    1L,
                    order.getAccountId(),
                    order.getSymbol(),
                    order.getQuantity(),
                    order.getSide(),
                    order.getOrderType(),
                    order.getPrice(),
                    order.getStatus(),
                    order.getRejectionReason(),
                    order.getBusinessRefId(),
                    order.getExecutedPrice(),
                    order.getExecutedQuantity(),
                    order.getCreatedAt(),
                    order.getUpdatedAt());
              });

      // When
      Order result = placeOrderService.placeOrder(command);

      // Then
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getAccountId()).isEqualTo(1L);
      assertThat(result.getSymbol()).isEqualTo("AAPL");
      assertThat(result.getStatus()).isEqualTo(OrderStatus.VALIDATED);
    }

    @Test
    @DisplayName("검증 실패 시 OrderValidationException을 던진다")
    void failure_throwsOrderValidationException_whenValidationFails() {
      // Given
      PlaceOrderCommand command =
          PlaceOrderCommand.of(
              1L,
              "AAPL",
              new BigDecimal("10"),
              OrderSide.BUY,
              OrderType.MARKET,
              new BigDecimal("150.00"),
              "ref-001");
      given(orderValidator.validate(any(Order.class)))
          .willReturn(Optional.of(RejectionReason.INSUFFICIENT_BALANCE));

      // When & Then
      assertThatThrownBy(() -> placeOrderService.placeOrder(command))
          .isInstanceOf(OrderValidationException.class)
          .extracting("reason")
          .isEqualTo(RejectionReason.INSUFFICIENT_BALANCE);
    }
  }
}
