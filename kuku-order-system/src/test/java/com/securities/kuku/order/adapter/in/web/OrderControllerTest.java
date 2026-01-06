package com.securities.kuku.order.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securities.kuku.order.adapter.in.web.dto.PlaceOrderRequest;
import com.securities.kuku.order.application.port.in.CancelOrderUseCase;
import com.securities.kuku.order.application.port.in.GetOrderUseCase;
import com.securities.kuku.order.application.port.in.PlaceOrderUseCase;
import com.securities.kuku.order.application.port.in.command.PlaceOrderCommand;
import com.securities.kuku.order.domain.InvalidOrderStateException;
import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderNotFoundException;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderStatus;
import com.securities.kuku.order.domain.OrderType;
import com.securities.kuku.order.domain.OrderValidationException;
import com.securities.kuku.order.domain.RejectionReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

  private static final Instant FIXED_TIME = Instant.parse("2025-01-06T02:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PlaceOrderUseCase placeOrderUseCase;
  @MockitoBean private GetOrderUseCase getOrderUseCase;
  @MockitoBean private CancelOrderUseCase cancelOrderUseCase;

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
  @DisplayName("POST /api/v1/orders")
  class PlaceOrder {

    @Test
    @DisplayName("유효한 요청 시 201 Created와 주문 정보를 반환한다")
    void success_returnsCreatedOrder() throws Exception {
      // Given
      PlaceOrderRequest request =
          new PlaceOrderRequest(
              1L,
              "AAPL",
              new BigDecimal("10"),
              "BUY",
              "MARKET",
              new BigDecimal("150.00"),
              "ref-001");
      Order order = createOrder(1L, OrderStatus.VALIDATED);
      given(placeOrderUseCase.placeOrder(any(PlaceOrderCommand.class))).willReturn(order);

      // When & Then
      mockMvc
          .perform(
              post("/api/v1/orders")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.orderId").value(1))
          .andExpect(jsonPath("$.symbol").value("AAPL"))
          .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("검증 실패 시 422 Unprocessable Entity를 반환한다")
    void failure_returns422_whenValidationFails() throws Exception {
      // Given
      PlaceOrderRequest request =
          new PlaceOrderRequest(
              1L,
              "AAPL",
              new BigDecimal("10"),
              "BUY",
              "MARKET",
              new BigDecimal("150.00"),
              "ref-001");
      given(placeOrderUseCase.placeOrder(any(PlaceOrderCommand.class)))
          .willThrow(new OrderValidationException(RejectionReason.INSUFFICIENT_BALANCE));

      // When & Then
      mockMvc
          .perform(
              post("/api/v1/orders")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("잘못된 side 값 시 400 Bad Request를 반환한다")
    void failure_returns400_whenInvalidSide() throws Exception {
      // Given
      PlaceOrderRequest request =
          new PlaceOrderRequest(
              1L,
              "AAPL",
              new BigDecimal("10"),
              "INVALID",
              "MARKET",
              new BigDecimal("150.00"),
              "ref-001");

      // When & Then
      mockMvc
          .perform(
              post("/api/v1/orders")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("accountId가 null이면 400 Bad Request를 반환한다")
    void failure_returns400_whenAccountIdIsNull() throws Exception {
      // Given
      PlaceOrderRequest request =
          new PlaceOrderRequest(
              null, // null accountId
              "AAPL",
              new BigDecimal("10"),
              "BUY",
              "MARKET",
              new BigDecimal("150.00"),
              "ref-001");

      // When & Then
      mockMvc
          .perform(
              post("/api/v1/orders")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.message").value("accountId: accountId is required"));
    }

    @Test
    @DisplayName("symbol이 빈 문자열이면 400 Bad Request를 반환한다")
    void failure_returns400_whenSymbolIsBlank() throws Exception {
      // Given
      PlaceOrderRequest request =
          new PlaceOrderRequest(
              1L,
              "", // blank symbol
              new BigDecimal("10"),
              "BUY",
              "MARKET",
              new BigDecimal("150.00"),
              "ref-001");

      // When & Then
      mockMvc
          .perform(
              post("/api/v1/orders")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.message").value("symbol: symbol is required"));
    }

    @Test
    @DisplayName("quantity가 음수이면 400 Bad Request를 반환한다")
    void failure_returns400_whenQuantityIsNegative() throws Exception {
      // Given
      PlaceOrderRequest request =
          new PlaceOrderRequest(
              1L,
              "AAPL",
              new BigDecimal("-10"), // negative quantity
              "BUY",
              "MARKET",
              new BigDecimal("150.00"),
              "ref-001");

      // When & Then
      mockMvc
          .perform(
              post("/api/v1/orders")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.message").value("quantity: quantity must be positive"));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/orders/{orderId}")
  class GetOrder {

    @Test
    @DisplayName("존재하는 주문 조회 시 200 OK와 주문 정보를 반환한다")
    void success_returnsOrder() throws Exception {
      // Given
      Long orderId = 1L;
      Order order = createOrder(orderId, OrderStatus.VALIDATED);
      given(getOrderUseCase.getOrder(orderId)).willReturn(Optional.of(order));

      // When & Then
      mockMvc
          .perform(get("/api/v1/orders/{orderId}", orderId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.orderId").value(orderId))
          .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 시 404 Not Found를 반환한다")
    void failure_returns404_whenOrderNotExists() throws Exception {
      // Given
      Long orderId = 999L;
      given(getOrderUseCase.getOrder(orderId)).willReturn(Optional.empty());

      // When & Then
      mockMvc
          .perform(get("/api/v1/orders/{orderId}", orderId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/orders/{orderId}/cancel")
  class CancelOrder {

    @Test
    @DisplayName("취소 성공 시 200 OK와 취소된 주문 정보를 반환한다")
    void success_returnsCancelledOrder() throws Exception {
      // Given
      Long orderId = 1L;
      Order cancelledOrder = createOrder(orderId, OrderStatus.CANCELLED);
      given(cancelOrderUseCase.cancelOrder(orderId)).willReturn(cancelledOrder);

      // When & Then
      mockMvc
          .perform(post("/api/v1/orders/{orderId}/cancel", orderId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.orderId").value(orderId))
          .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("존재하지 않는 주문 취소 시 404 Not Found를 반환한다")
    void failure_returns404_whenOrderNotExists() throws Exception {
      // Given
      Long orderId = 999L;
      given(cancelOrderUseCase.cancelOrder(orderId)).willThrow(new OrderNotFoundException(orderId));

      // When & Then
      mockMvc
          .perform(post("/api/v1/orders/{orderId}/cancel", orderId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 체결된 주문 취소 시 409 Conflict를 반환한다")
    void failure_returns409_whenOrderAlreadyFilled() throws Exception {
      // Given
      Long orderId = 1L;
      given(cancelOrderUseCase.cancelOrder(orderId))
          .willThrow(new InvalidOrderStateException("Cannot cancel order in FILLED status"));

      // When & Then
      mockMvc
          .perform(post("/api/v1/orders/{orderId}/cancel", orderId))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
  }
}
