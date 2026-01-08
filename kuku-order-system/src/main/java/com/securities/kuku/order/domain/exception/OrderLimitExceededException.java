package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.BusinessException;

/** 주문 한도를 초과했을 때 발생하는 예외. */
public class OrderLimitExceededException extends BusinessException {

  private final Long accountId;
  private final String limitType;

  public OrderLimitExceededException(Long accountId, String limitType, String message) {
    super(OrderErrorCode.ORDER_LIMIT_EXCEEDED, message);
    this.accountId = accountId;
    this.limitType = limitType;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getLimitType() {
    return limitType;
  }
}
