package com.securities.kuku.order.domain;

public class InvalidOrderStateException extends RuntimeException {

  public InvalidOrderStateException(String message) {
    super(message);
  }
}
