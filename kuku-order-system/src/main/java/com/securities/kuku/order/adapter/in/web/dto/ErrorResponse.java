package com.securities.kuku.order.adapter.in.web.dto;

import java.time.Instant;

public record ErrorResponse(String error, String message, Instant timestamp) {

  public static ErrorResponse of(String error, String message) {
    return of(error, message, Instant.now());
  }

  public static ErrorResponse of(String error, String message, Instant timestamp) {
    return new ErrorResponse(error, message, timestamp);
  }
}
