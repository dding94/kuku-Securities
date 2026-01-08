package com.securities.kuku.order.adapter.in.web;

import com.securities.kuku.common.exception.BusinessException;
import com.securities.kuku.common.exception.CommonErrorCode;
import com.securities.kuku.common.exception.ErrorResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Order 서비스 Global Exception Handler. */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class OrderExceptionHandler {

  private final Clock clock;

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
    log.warn("Business exception occurred: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());

    ErrorResponse response =
        ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), getTrackingId(), now());
    return ResponseEntity.status(ex.getErrorCode().getStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Validation failed");

    log.warn("Request validation failed: {}", message);

    ErrorResponse response =
        ErrorResponse.of(CommonErrorCode.VALIDATION_FAILED, message, getTrackingId(), now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Invalid argument: {}", ex.getMessage());

    ErrorResponse response =
        ErrorResponse.of(CommonErrorCode.INVALID_REQUEST, ex.getMessage(), getTrackingId(), now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Unexpected error occurred", ex);

    ErrorResponse response =
        ErrorResponse.of(CommonErrorCode.INTERNAL_ERROR, getTrackingId(), now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  private Instant now() {
    return Instant.now(clock);
  }

  private String getTrackingId() {
    String traceId = MDC.get("traceId");
    return traceId != null ? traceId : UUID.randomUUID().toString();
  }
}
