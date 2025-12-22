package com.securities.kuku.ledger.application.port.in.command;

public record ReversalCommand(Long originalTransactionId, String reason) {

  public ReversalCommand {
    if (originalTransactionId == null) {
      throw new IllegalArgumentException("Original Transaction ID cannot be null");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Reason cannot be null or empty");
    }
  }

  public static ReversalCommand of(Long originalTransactionId, String reason) {
    return new ReversalCommand(originalTransactionId, reason);
  }
}
