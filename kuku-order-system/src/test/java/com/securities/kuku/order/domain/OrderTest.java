package com.securities.kuku.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-01T03:00:00Z");
  private static final Instant UPDATED_TIME = Instant.parse("2025-01-01T03:01:00Z");
  private static final Long ACCOUNT_ID = 100L;
  private static final String SYMBOL = "005930";
  private static final BigDecimal QUANTITY = new BigDecimal("10");
  private static final BigDecimal PRICE = new BigDecimal("70000");

  private Order createOrder(OrderStatus status) {
    return new Order(
        1L,
        ACCOUNT_ID,
        SYMBOL,
        QUANTITY,
        OrderSide.BUY,
        OrderType.MARKET,
        PRICE,
        status,
        status == OrderStatus.REJECTED ? RejectionReason.INSUFFICIENT_BALANCE : null,
        "REF-001",
        null,
        null,
        FIXED_TIME,
        FIXED_TIME);
  }

  @Nested
  @DisplayName("생성자 유효성 검증")
  class ConstructorValidation {

    @Test
    @DisplayName("정상적인 주문 생성")
    void success_whenAllFieldsValid() {
      Order order =
          new Order(
              1L,
              ACCOUNT_ID,
              SYMBOL,
              QUANTITY,
              OrderSide.BUY,
              OrderType.MARKET,
              PRICE,
              OrderStatus.CREATED,
              null,
              "REF-001",
              null,
              null,
              FIXED_TIME,
              FIXED_TIME);

      assertThat(order.getId()).isEqualTo(1L);
      assertThat(order.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(order.getSymbol()).isEqualTo(SYMBOL);
      assertThat(order.getQuantity()).isEqualByComparingTo(QUANTITY);
      assertThat(order.getSide()).isEqualTo(OrderSide.BUY);
      assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
      assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("accountId가 null이면 예외 발생")
    void throwsException_whenAccountIdIsNull() {
      assertThatThrownBy(
              () ->
                  new Order(
                      1L,
                      null,
                      SYMBOL,
                      QUANTITY,
                      OrderSide.BUY,
                      OrderType.MARKET,
                      PRICE,
                      OrderStatus.CREATED,
                      null,
                      "REF-001",
                      null,
                      null,
                      FIXED_TIME,
                      FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("AccountId");
    }

    @Test
    @DisplayName("symbol이 null이면 예외 발생")
    void throwsException_whenSymbolIsNull() {
      assertThatThrownBy(
              () ->
                  new Order(
                      1L,
                      ACCOUNT_ID,
                      null,
                      QUANTITY,
                      OrderSide.BUY,
                      OrderType.MARKET,
                      PRICE,
                      OrderStatus.CREATED,
                      null,
                      "REF-001",
                      null,
                      null,
                      FIXED_TIME,
                      FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Symbol");
    }

    @Test
    @DisplayName("quantity가 0 이하면 예외 발생")
    void throwsException_whenQuantityIsZeroOrNegative() {
      assertThatThrownBy(
              () ->
                  new Order(
                      1L,
                      ACCOUNT_ID,
                      SYMBOL,
                      BigDecimal.ZERO,
                      OrderSide.BUY,
                      OrderType.MARKET,
                      PRICE,
                      OrderStatus.CREATED,
                      null,
                      "REF-001",
                      null,
                      null,
                      FIXED_TIME,
                      FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity");
    }

    @Test
    @DisplayName("REJECTED 상태에서 rejectionReason이 null이면 예외 발생")
    void throwsException_whenRejectedWithoutReason() {
      assertThatThrownBy(
              () ->
                  new Order(
                      1L,
                      ACCOUNT_ID,
                      SYMBOL,
                      QUANTITY,
                      OrderSide.BUY,
                      OrderType.MARKET,
                      PRICE,
                      OrderStatus.REJECTED,
                      null,
                      "REF-001",
                      null,
                      null,
                      FIXED_TIME,
                      FIXED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("RejectionReason");
    }

    @Test
    @DisplayName("id는 null을 허용한다 (신규 생성 시)")
    void allowsNullId() {
      Order order =
          new Order(
              null,
              ACCOUNT_ID,
              SYMBOL,
              QUANTITY,
              OrderSide.BUY,
              OrderType.MARKET,
              PRICE,
              OrderStatus.CREATED,
              null,
              "REF-001",
              null,
              null,
              FIXED_TIME,
              FIXED_TIME);

      assertThat(order.getId()).isNull();
    }

    @Test
    @DisplayName("price는 null을 허용한다 (시장가 주문 시)")
    void allowsNullPrice() {
      Order order =
          new Order(
              1L,
              ACCOUNT_ID,
              SYMBOL,
              QUANTITY,
              OrderSide.BUY,
              OrderType.MARKET,
              null,
              OrderStatus.CREATED,
              null,
              "REF-001",
              null,
              null,
              FIXED_TIME,
              FIXED_TIME);

      assertThat(order.getPrice()).isNull();
    }
  }

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class FactoryMethods {

    @Test
    @DisplayName("create()는 CREATED 상태로 주문을 생성한다")
    void create_success() {
      Order order =
          Order.create(
              ACCOUNT_ID,
              SYMBOL,
              QUANTITY,
              OrderSide.BUY,
              OrderType.MARKET,
              PRICE,
              "REF-001",
              FIXED_TIME);

      assertThat(order.getId()).isNull();
      assertThat(order.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(order.getSymbol()).isEqualTo(SYMBOL);
      assertThat(order.getQuantity()).isEqualByComparingTo(QUANTITY);
      assertThat(order.getSide()).isEqualTo(OrderSide.BUY);
      assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
      assertThat(order.getPrice()).isEqualByComparingTo(PRICE);
      assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
      assertThat(order.getRejectionReason()).isNull();
      assertThat(order.getBusinessRefId()).isEqualTo("REF-001");
      assertThat(order.getCreatedAt()).isEqualTo(FIXED_TIME);
      assertThat(order.getUpdatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("create()로 매도 주문 생성")
    void create_sellOrder() {
      Order order =
          Order.create(
              ACCOUNT_ID,
              SYMBOL,
              QUANTITY,
              OrderSide.SELL,
              OrderType.MARKET,
              null,
              "REF-002",
              FIXED_TIME);

      assertThat(order.getSide()).isEqualTo(OrderSide.SELL);
      assertThat(order.getPrice()).isNull();
    }
  }

  @Nested
  @DisplayName("validate() 메서드")
  class Validate {

    @Test
    @DisplayName("CREATED 상태에서 VALIDATED로 전환")
    void success_whenStatusIsCreated() {
      Order created = createOrder(OrderStatus.CREATED);

      Order validated = created.validate(UPDATED_TIME);

      assertThat(validated).isNotSameAs(created);
      assertThat(validated.getStatus()).isEqualTo(OrderStatus.VALIDATED);
      assertThat(validated.getUpdatedAt()).isEqualTo(UPDATED_TIME);
      assertThat(validated.getCreatedAt()).isEqualTo(FIXED_TIME);
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"VALIDATED", "FILLED", "REJECTED", "CANCELLED"})
    @DisplayName("CREATED가 아닌 상태에서 validate() 호출 시 예외")
    void throwsException_whenStatusIsNotCreated(OrderStatus status) {
      Order order = createOrder(status);

      assertThatThrownBy(() -> order.validate(UPDATED_TIME))
          .isInstanceOf(InvalidOrderStateException.class)
          .hasMessageContaining("CREATED");
    }
  }

  @Nested
  @DisplayName("reject() 메서드")
  class Reject {

    @Test
    @DisplayName("CREATED 상태에서 REJECTED로 전환")
    void success_whenStatusIsCreated() {
      Order created = createOrder(OrderStatus.CREATED);

      Order rejected = created.reject(RejectionReason.INSUFFICIENT_BALANCE, UPDATED_TIME);

      assertThat(rejected).isNotSameAs(created);
      assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
      assertThat(rejected.getRejectionReason()).isEqualTo(RejectionReason.INSUFFICIENT_BALANCE);
      assertThat(rejected.getUpdatedAt()).isEqualTo(UPDATED_TIME);
    }

    @Test
    @DisplayName("VALIDATED 상태에서 REJECTED로 전환")
    void success_whenStatusIsValidated() {
      Order validated = createOrder(OrderStatus.VALIDATED);

      Order rejected = validated.reject(RejectionReason.MARKET_CLOSED, UPDATED_TIME);

      assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
      assertThat(rejected.getRejectionReason()).isEqualTo(RejectionReason.MARKET_CLOSED);
    }

    @Test
    @DisplayName("rejectionReason이 null이면 예외 발생")
    void throwsException_whenReasonIsNull() {
      Order created = createOrder(OrderStatus.CREATED);

      assertThatThrownBy(() -> created.reject(null, UPDATED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("RejectionReason");
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"FILLED", "REJECTED", "CANCELLED"})
    @DisplayName("종료 상태에서 reject() 호출 시 예외")
    void throwsException_whenStatusIsTerminal(OrderStatus status) {
      Order order = createOrder(status);

      assertThatThrownBy(() -> order.reject(RejectionReason.MARKET_CLOSED, UPDATED_TIME))
          .isInstanceOf(InvalidOrderStateException.class);
    }
  }

  @Nested
  @DisplayName("fill() 메서드")
  class Fill {

    private static final BigDecimal EXECUTED_PRICE = new BigDecimal("70100");
    private static final BigDecimal EXECUTED_QUANTITY = new BigDecimal("10");

    @Test
    @DisplayName("VALIDATED 상태에서 FILLED로 전환")
    void success_whenStatusIsValidated() {
      Order validated = createOrder(OrderStatus.VALIDATED);

      Order filled = validated.fill(EXECUTED_PRICE, EXECUTED_QUANTITY, UPDATED_TIME);

      assertThat(filled).isNotSameAs(validated);
      assertThat(filled.getStatus()).isEqualTo(OrderStatus.FILLED);
      assertThat(filled.getExecutedPrice()).isEqualByComparingTo(EXECUTED_PRICE);
      assertThat(filled.getExecutedQuantity()).isEqualByComparingTo(EXECUTED_QUANTITY);
      assertThat(filled.getUpdatedAt()).isEqualTo(UPDATED_TIME);
    }

    @Test
    @DisplayName("executedPrice가 0 이하면 예외 발생")
    void throwsException_whenExecutedPriceIsZeroOrNegative() {
      Order validated = createOrder(OrderStatus.VALIDATED);

      assertThatThrownBy(() -> validated.fill(BigDecimal.ZERO, EXECUTED_QUANTITY, UPDATED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ExecutedPrice");
    }

    @Test
    @DisplayName("executedQuantity가 0 이하면 예외 발생")
    void throwsException_whenExecutedQuantityIsZeroOrNegative() {
      Order validated = createOrder(OrderStatus.VALIDATED);

      assertThatThrownBy(() -> validated.fill(EXECUTED_PRICE, BigDecimal.ZERO, UPDATED_TIME))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ExecutedQuantity");
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"CREATED", "FILLED", "REJECTED", "CANCELLED"})
    @DisplayName("VALIDATED가 아닌 상태에서 fill() 호출 시 예외")
    void throwsException_whenStatusIsNotValidated(OrderStatus status) {
      Order order = createOrder(status);

      assertThatThrownBy(() -> order.fill(EXECUTED_PRICE, EXECUTED_QUANTITY, UPDATED_TIME))
          .isInstanceOf(InvalidOrderStateException.class)
          .hasMessageContaining("VALIDATED");
    }
  }

  @Nested
  @DisplayName("cancel() 메서드")
  class Cancel {

    @Test
    @DisplayName("VALIDATED 상태에서 CANCELLED로 전환")
    void success_whenStatusIsValidated() {
      Order validated = createOrder(OrderStatus.VALIDATED);

      Order cancelled = validated.cancel(UPDATED_TIME);

      assertThat(cancelled).isNotSameAs(validated);
      assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(cancelled.getUpdatedAt()).isEqualTo(UPDATED_TIME);
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"CREATED", "FILLED", "REJECTED", "CANCELLED"})
    @DisplayName("VALIDATED가 아닌 상태에서 cancel() 호출 시 예외")
    void throwsException_whenStatusIsNotValidated(OrderStatus status) {
      Order order = createOrder(status);

      assertThatThrownBy(() -> order.cancel(UPDATED_TIME))
          .isInstanceOf(InvalidOrderStateException.class)
          .hasMessageContaining("VALIDATED");
    }
  }
}
