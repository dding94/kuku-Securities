package com.securities.kuku.order.application.port.in.command;

import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderType;
import java.math.BigDecimal;

public record PlaceOrderCommand(
    Long accountId,
    String symbol,
    BigDecimal quantity,
    OrderSide side,
    OrderType orderType,
    BigDecimal price,
    String businessRefId) {

  public PlaceOrderCommand {
    if (accountId == null) {
      throw new IllegalArgumentException("AccountId cannot be null");
    }
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Symbol cannot be null or blank");
    }
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    if (side == null) {
      throw new IllegalArgumentException("OrderSide cannot be null");
    }
    if (orderType == null) {
      throw new IllegalArgumentException("OrderType cannot be null");
    }
  }

  public static PlaceOrderCommand of(
      Long accountId,
      String symbol,
      BigDecimal quantity,
      OrderSide side,
      OrderType orderType,
      BigDecimal price,
      String businessRefId) {
    return new PlaceOrderCommand(
        accountId, symbol, quantity, side, orderType, price, businessRefId);
  }
}
