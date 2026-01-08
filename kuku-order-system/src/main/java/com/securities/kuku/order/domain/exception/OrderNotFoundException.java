package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.BusinessException;

/** 주문을 찾을 수 없을 때 발생하는 예외. */
public class OrderNotFoundException extends BusinessException {

  private final Long orderId;

  public OrderNotFoundException(Long orderId) {
    super(OrderErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId);
    this.orderId = orderId;
  }

  public Long getOrderId() {
    return orderId;
  }
}
