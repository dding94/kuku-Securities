package com.securities.kuku.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Order {

  private final Long id;
  private final Long accountId;
  private final String symbol;
  private final BigDecimal quantity;
  private final OrderSide side;
  private final OrderType orderType;
  private final BigDecimal price;
  private final OrderStatus status;
  private final RejectionReason rejectionReason;
  private final String businessRefId;
  private final BigDecimal executedPrice;
  private final BigDecimal executedQuantity;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Order(
      Long id,
      Long accountId,
      String symbol,
      BigDecimal quantity,
      OrderSide side,
      OrderType orderType,
      BigDecimal price,
      OrderStatus status,
      RejectionReason rejectionReason,
      String businessRefId,
      BigDecimal executedPrice,
      BigDecimal executedQuantity,
      Instant createdAt,
      Instant updatedAt) {

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
    if (status == null) {
      throw new IllegalArgumentException("OrderStatus cannot be null");
    }
    if (createdAt == null) {
      throw new IllegalArgumentException("CreatedAt cannot be null");
    }
    if (updatedAt == null) {
      throw new IllegalArgumentException("UpdatedAt cannot be null");
    }
    if (status == OrderStatus.REJECTED && rejectionReason == null) {
      throw new IllegalArgumentException("RejectionReason is required for REJECTED status");
    }

    this.id = id;
    this.accountId = accountId;
    this.symbol = symbol;
    this.quantity = quantity;
    this.side = side;
    this.orderType = orderType;
    this.price = price;
    this.status = status;
    this.rejectionReason = rejectionReason;
    this.businessRefId = businessRefId;
    this.executedPrice = executedPrice;
    this.executedQuantity = executedQuantity;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Order create(
      Long accountId,
      String symbol,
      BigDecimal quantity,
      OrderSide side,
      OrderType orderType,
      BigDecimal price,
      String businessRefId,
      Instant now) {

    return new Order(
        null,
        accountId,
        symbol,
        quantity,
        side,
        orderType,
        price,
        OrderStatus.CREATED,
        null,
        businessRefId,
        null,
        null,
        now,
        now);
  }

  public Order validate(Instant now) {
    if (!this.status.canTransitionTo(OrderStatus.VALIDATED)) {
      throw new InvalidOrderStateException("Cannot validate order in " + this.status + " status");
    }
    return withStatusAndTime(OrderStatus.VALIDATED, now);
  }

  public Order reject(RejectionReason reason, Instant now) {
    if (reason == null) {
      throw new IllegalArgumentException("RejectionReason cannot be null");
    }
    if (!this.status.canTransitionTo(OrderStatus.REJECTED)) {
      throw new InvalidOrderStateException("Cannot reject order in " + this.status + " status");
    }
    return new Order(
        this.id,
        this.accountId,
        this.symbol,
        this.quantity,
        this.side,
        this.orderType,
        this.price,
        OrderStatus.REJECTED,
        reason,
        this.businessRefId,
        this.executedPrice,
        this.executedQuantity,
        this.createdAt,
        now);
  }

  public Order fill(BigDecimal executedPrice, BigDecimal executedQuantity, Instant now) {
    if (!this.status.canTransitionTo(OrderStatus.FILLED)) {
      throw new InvalidOrderStateException("Cannot fill order in " + this.status + " status");
    }
    if (executedPrice == null || executedPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("ExecutedPrice must be greater than zero");
    }
    if (executedQuantity == null || executedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("ExecutedQuantity must be greater than zero");
    }
    return new Order(
        this.id,
        this.accountId,
        this.symbol,
        this.quantity,
        this.side,
        this.orderType,
        this.price,
        OrderStatus.FILLED,
        this.rejectionReason,
        this.businessRefId,
        executedPrice,
        executedQuantity,
        this.createdAt,
        now);
  }

  public Order cancel(Instant now) {
    if (!this.status.canTransitionTo(OrderStatus.CANCELLED)) {
      throw new InvalidOrderStateException("Cannot cancel order in " + this.status + " status");
    }
    return withStatusAndTime(OrderStatus.CANCELLED, now);
  }

  private Order withStatusAndTime(OrderStatus newStatus, Instant updatedAt) {
    return new Order(
        this.id,
        this.accountId,
        this.symbol,
        this.quantity,
        this.side,
        this.orderType,
        this.price,
        newStatus,
        this.rejectionReason,
        this.businessRefId,
        this.executedPrice,
        this.executedQuantity,
        this.createdAt,
        updatedAt);
  }
}
