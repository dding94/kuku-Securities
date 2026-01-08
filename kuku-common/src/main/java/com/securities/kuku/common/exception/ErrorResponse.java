package com.securities.kuku.common.exception;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/** 클라이언트에게 반환될 표준 에러 응답 포맷. */
@Getter
@Builder
public class ErrorResponse {

  private final String code;
  private final String message;
  private final int status;
  private final Instant timestamp;
  private final String trackingId;

  public static ErrorResponse of(ErrorCode errorCode, String trackingId, Instant timestamp) {
    return ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .status(errorCode.getStatus())
        .timestamp(timestamp)
        .trackingId(trackingId)
        .build();
  }

  public static ErrorResponse of(
      ErrorCode errorCode, String message, String trackingId, Instant timestamp) {
    return ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(message)
        .status(errorCode.getStatus())
        .timestamp(timestamp)
        .trackingId(trackingId)
        .build();
  }

  public static ErrorResponse of(
      String code, String message, int status, String trackingId, Instant timestamp) {
    return ErrorResponse.builder()
        .code(code)
        .message(message)
        .status(status)
        .timestamp(timestamp)
        .trackingId(trackingId)
        .build();
  }
}
