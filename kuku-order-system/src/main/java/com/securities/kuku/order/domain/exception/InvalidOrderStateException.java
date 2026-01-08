package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.BusinessException;

/** 주문 상태 전환이 유효하지 않을 때 발생하는 예외. */
public class InvalidOrderStateException extends BusinessException {

  public InvalidOrderStateException(String message) {
    super(OrderErrorCode.INVALID_ORDER_STATE, message);
  }
}
