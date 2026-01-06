package com.securities.kuku.order.domain;

public enum OrderStatus {
  CREATED,
  VALIDATED,
  FILLED,
  REJECTED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatus target) {
    return switch (this) {
      case CREATED -> target == VALIDATED || target == REJECTED;
      case VALIDATED -> target == FILLED || target == REJECTED || target == CANCELLED;
      case FILLED, REJECTED, CANCELLED -> false;
    };
  }

  public boolean isTerminal() {
    return switch (this) {
      case FILLED, REJECTED, CANCELLED -> true;
      case CREATED, VALIDATED -> false;
    };
  }

  public boolean isSuccessful() {
    return switch (this) {
      case VALIDATED, FILLED -> true;
      case CREATED, REJECTED, CANCELLED -> false;
    };
  }
}
