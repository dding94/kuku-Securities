package com.securities.kuku.order.adapter.in.web.dto;

import com.securities.kuku.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    Long orderId,
    Long accountId,
    String symbol,
    BigDecimal quantity,
    String side,
    String orderType,
    BigDecimal price,
    String status,
    String rejectedReason,
    String businessRefId,
    Instant createdAt,
    Instant updatedAt) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getAccountId(),
        order.getSymbol(),
        order.getQuantity(),
        order.getSide().name(),
        order.getOrderType().name(),
        order.getPrice(),
        order.getStatus().name(),
        order.getRejectionReason() != null ? order.getRejectionReason().name() : null,
        order.getBusinessRefId(),
        order.getCreatedAt(),
        order.getUpdatedAt());
  }
}
