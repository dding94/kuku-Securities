package com.securities.kuku.order.domain.exception;

import com.securities.kuku.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 주문 도메인 에러 코드. */
@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
  ORDER_NOT_FOUND("ORDER_001", "Order not found", 404),
  INVALID_ORDER_STATE("ORDER_002", "Invalid order state", 409),
  ORDER_VALIDATION_FAILED("ORDER_003", "Order validation failed", 422),
  ORDER_LIMIT_EXCEEDED("ORDER_004", "Order limit exceeded", 422),
  INVALID_ORDER_SIDE("ORDER_005", "Invalid order side", 400),
  INVALID_ORDER_TYPE("ORDER_006", "Invalid order type", 400);

  private final String code;
  private final String message;
  private final int status;
}
