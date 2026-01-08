package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.BusinessException;

/** 유효하지 않은 주문 방향(side) 값일 때 발생하는 예외. */
public class InvalidOrderSideException extends BusinessException {

  private final String invalidValue;

  public InvalidOrderSideException(String invalidValue) {
    super(OrderErrorCode.INVALID_ORDER_SIDE, "Invalid order side: " + invalidValue);
    this.invalidValue = invalidValue;
  }

  public String getInvalidValue() {
    return invalidValue;
  }
}
