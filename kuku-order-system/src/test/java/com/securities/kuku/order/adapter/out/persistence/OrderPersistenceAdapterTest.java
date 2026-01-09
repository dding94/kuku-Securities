package com.securities.kuku.order.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderStatus;
import com.securities.kuku.order.domain.OrderType;
import com.securities.kuku.order.domain.RejectionReason;
import com.securities.kuku.order.domain.exception.OrderNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(OrderPersistenceAdapter.class)
class OrderPersistenceAdapterTest {

  @Autowired private OrderPersistenceAdapter adapter;

  private static final Instant FIXED_TIME = Instant.parse("2026-01-09T06:00:00Z");

  private Order createOrder() {
    return Order.create(
        1001L,
        "AAPL",
        new BigDecimal("10.00000000"),
        OrderSide.BUY,
        OrderType.MARKET,
        null,
        "REF-001",
        FIXED_TIME);
  }

  private Order createOrderWithBusinessRefId(String businessRefId) {
    return Order.create(
        1001L,
        "AAPL",
        new BigDecimal("10.00000000"),
        OrderSide.BUY,
        OrderType.MARKET,
        null,
        businessRefId,
        FIXED_TIME);
  }

  @Nested
  @DisplayName("save()")
  class Save {

    @Test
    @DisplayName("주문 저장 시 ID가 할당된다")
    void shouldSaveOrderAndAssignId() {
      // Given
      Order order = createOrder();

      // When
      Order saved = adapter.save(order);

      // Then
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getId()).isPositive();
    }

    @Test
    @DisplayName("저장된 주문의 모든 필드가 올바르게 영속화된다")
    void shouldPersistAllFieldsCorrectly() {
      // Given
      Order order = createOrder();

      // When
      Order saved = adapter.save(order);

      // Then
      assertThat(saved.getAccountId()).isEqualTo(1001L);
      assertThat(saved.getSymbol()).isEqualTo("AAPL");
      assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("10.00000000"));
      assertThat(saved.getSide()).isEqualTo(OrderSide.BUY);
      assertThat(saved.getOrderType()).isEqualTo(OrderType.MARKET);
      assertThat(saved.getPrice()).isNull();
      assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
      assertThat(saved.getRejectionReason()).isNull();
      assertThat(saved.getBusinessRefId()).isEqualTo("REF-001");
      assertThat(saved.getCreatedAt()).isEqualTo(FIXED_TIME);
      assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_TIME);
    }
  }

  @Nested
  @DisplayName("findById()")
  class FindById {

    @Test
    @DisplayName("존재하는 주문 ID로 조회 시 주문을 반환한다")
    void shouldFindOrderById() {
      // Given
      Order saved = adapter.save(createOrder());

      // When
      var found = adapter.findById(saved.getId());

      // Then
      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
      assertThat(found.get().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 조회 시 빈 Optional을 반환한다")
    void shouldReturnEmptyWhenOrderNotFound() {
      // When
      var found = adapter.findById(999999L);

      // Then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("update()")
  class Update {

    @Test
    @DisplayName("주문 상태 변경이 영속화된다")
    void shouldUpdateOrderStatus() {
      // Given
      Order saved = adapter.save(createOrder());
      Order validated = saved.validate(FIXED_TIME.plusSeconds(60));

      // When
      Order updated = adapter.update(validated);

      // Then
      assertThat(updated.getStatus()).isEqualTo(OrderStatus.VALIDATED);
      assertThat(updated.getUpdatedAt()).isEqualTo(FIXED_TIME.plusSeconds(60));
    }

    @Test
    @DisplayName("거부 사유가 영속화된다")
    void shouldUpdateRejectionReason() {
      // Given
      Order saved = adapter.save(createOrder());
      Order rejected =
          saved.reject(RejectionReason.INSUFFICIENT_BALANCE, FIXED_TIME.plusSeconds(60));

      // When
      Order updated = adapter.update(rejected);

      // Then
      assertThat(updated.getStatus()).isEqualTo(OrderStatus.REJECTED);
      assertThat(updated.getRejectionReason()).isEqualTo(RejectionReason.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("체결 정보가 영속화된다")
    void shouldUpdateExecutionInfo() {
      // Given
      Order saved = adapter.save(createOrder());
      Order validated = saved.validate(FIXED_TIME.plusSeconds(30));
      adapter.update(validated);

      Order filled =
          validated.fill(
              new BigDecimal("150.50000000"),
              new BigDecimal("10.00000000"),
              FIXED_TIME.plusSeconds(60));

      // When
      Order updated = adapter.update(filled);

      // Then
      assertThat(updated.getStatus()).isEqualTo(OrderStatus.FILLED);
      assertThat(updated.getExecutedPrice()).isEqualByComparingTo(new BigDecimal("150.50000000"));
      assertThat(updated.getExecutedQuantity()).isEqualByComparingTo(new BigDecimal("10.00000000"));
    }

    @Test
    @DisplayName("존재하지 않는 주문 업데이트 시 OrderNotFoundException 발생")
    void shouldThrowExceptionWhenOrderNotFound() {
      // Given
      Order nonExistentOrder =
          new Order(
              999999L,
              1001L,
              "AAPL",
              new BigDecimal("10"),
              OrderSide.BUY,
              OrderType.MARKET,
              null,
              OrderStatus.VALIDATED,
              null,
              "REF-999",
              null,
              null,
              FIXED_TIME,
              FIXED_TIME);

      // When & Then
      assertThatThrownBy(() -> adapter.update(nonExistentOrder))
          .isInstanceOf(OrderNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Enum 매핑")
  class EnumMapping {

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    @DisplayName("OrderSide Enum이 올바르게 저장/조회된다")
    void shouldMapOrderSideCorrectly(OrderSide side) {
      // Given
      Order order =
          Order.create(
              1001L,
              "AAPL",
              new BigDecimal("10"),
              side,
              OrderType.MARKET,
              null,
              "REF-SIDE-" + side.name(),
              FIXED_TIME);

      // When
      Order saved = adapter.save(order);
      var found = adapter.findById(saved.getId());

      // Then
      assertThat(found).isPresent();
      assertThat(found.get().getSide()).isEqualTo(side);
    }

    @ParameterizedTest
    @EnumSource(
        value = RejectionReason.class,
        names = {"INSUFFICIENT_BALANCE", "INSUFFICIENT_QUANTITY", "MARKET_CLOSED"})
    @DisplayName("RejectionReason Enum이 올바르게 저장/조회된다")
    void shouldMapRejectionReasonCorrectly(RejectionReason reason) {
      // Given
      Order order = createOrderWithBusinessRefId("REF-REASON-" + reason.name());
      Order saved = adapter.save(order);
      Order rejected = saved.reject(reason, FIXED_TIME.plusSeconds(30));

      // When
      Order updated = adapter.update(rejected);
      var found = adapter.findById(updated.getId());

      // Then
      assertThat(found).isPresent();
      assertThat(found.get().getRejectionReason()).isEqualTo(reason);
    }
  }

  @Nested
  @DisplayName("businessRefId UNIQUE 제약조건")
  class BusinessRefIdConstraint {

    @Test
    @DisplayName("중복 businessRefId 저장 시 예외 발생")
    void shouldRejectDuplicateBusinessRefId() {
      // Given
      Order first = createOrderWithBusinessRefId("DUPLICATE-REF");
      adapter.save(first);

      Order second = createOrderWithBusinessRefId("DUPLICATE-REF");

      // When & Then
      assertThatThrownBy(() -> adapter.save(second))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
  }
}
