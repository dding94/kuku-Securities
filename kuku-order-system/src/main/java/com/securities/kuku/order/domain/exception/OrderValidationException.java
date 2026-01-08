package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.BusinessException;
import com.securities.kuku.order.domain.RejectionReason;
import lombok.Getter;

/** 주문 검증 실패 시 발생하는 예외. */
@Getter
public class OrderValidationException extends BusinessException {

  private final RejectionReason reason;

  public OrderValidationException(RejectionReason reason) {
    super(OrderErrorCode.ORDER_VALIDATION_FAILED, "Order validation failed: " + reason.name());
    this.reason = reason;
  }
}
