package com.securities.kuku.order.adapter.out.persistence;

import com.securities.kuku.order.domain.Order;
import com.securities.kuku.order.domain.OrderSide;
import com.securities.kuku.order.domain.OrderStatus;
import com.securities.kuku.order.domain.OrderType;
import com.securities.kuku.order.domain.RejectionReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "symbol", nullable = false, length = 20)
  private String symbol;

  @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
  private BigDecimal quantity;

  @Enumerated(EnumType.STRING)
  @Column(name = "side", nullable = false, length = 10)
  private OrderSide side;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_type", nullable = false, length = 10)
  private OrderType orderType;

  @Column(name = "price", precision = 18, scale = 8)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "rejected_reason", length = 50)
  private RejectionReason rejectionReason;

  @Column(name = "business_ref_id", unique = true, length = 100)
  private String businessRefId;

  @Column(name = "executed_price", precision = 18, scale = 8)
  private BigDecimal executedPrice;

  @Column(name = "executed_quantity", precision = 18, scale = 8)
  private BigDecimal executedQuantity;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public OrderJpaEntity(
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

  public Order toDomain() {
    return new Order(
        id,
        accountId,
        symbol,
        quantity,
        side,
        orderType,
        price,
        status,
        rejectionReason,
        businessRefId,
        executedPrice,
        executedQuantity,
        createdAt,
        updatedAt);
  }

  public static OrderJpaEntity fromDomain(Order order) {
    return new OrderJpaEntity(
        order.getId(),
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
  }

  public void updateFrom(Order order) {
    this.status = order.getStatus();
    this.rejectionReason = order.getRejectionReason();
    this.executedPrice = order.getExecutedPrice();
    this.executedQuantity = order.getExecutedQuantity();
    this.updatedAt = order.getUpdatedAt();
  }
}
