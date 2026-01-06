package com.securities.kuku.order.domain;

public class OrderValidationException extends RuntimeException {

  private final RejectionReason reason;

  public OrderValidationException(RejectionReason reason) {
    super("Order validation failed: " + reason.name());
    this.reason = reason;
  }

  public RejectionReason getReason() {
    return reason;
  }
}
