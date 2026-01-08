package com.securities.kuku.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 시스템 공통 에러 코드. */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
  INTERNAL_ERROR("COMMON_001", "An unexpected error occurred", 500),
  INVALID_REQUEST("COMMON_002", "Invalid request", 400),
  VALIDATION_FAILED("COMMON_003", "Validation failed", 400),
  RESOURCE_NOT_FOUND("COMMON_004", "Resource not found", 404),
  SERVICE_UNAVAILABLE("COMMON_005", "Service temporarily unavailable", 503);

  private final String code;
  private final String message;
  private final int status;
}
