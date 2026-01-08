package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.BusinessException;

/** 유효하지 않은 주문 유형(type) 값일 때 발생하는 예외. */
public class InvalidOrderTypeException extends BusinessException {

  private final String invalidValue;

  public InvalidOrderTypeException(String invalidValue) {
    super(OrderErrorCode.INVALID_ORDER_TYPE, "Invalid order type: " + invalidValue);
    this.invalidValue = invalidValue;
  }

  public String getInvalidValue() {
    return invalidValue;
  }
}
