package com.securities.kuku.ledger.domain.event;

import java.time.Instant;

public record LedgerReversedEvent(
    Long reversalTransactionId, Long originalTransactionId, String reason, Instant occurredAt)
    implements LedgerEvent {

  private static final String EVENT_TYPE = "LEDGER_REVERSED";

  public static LedgerReversedEvent of(
      Long reversalTransactionId, Long originalTransactionId, String reason, Instant occurredAt) {
    return new LedgerReversedEvent(
        reversalTransactionId, originalTransactionId, reason, occurredAt);
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public Long aggregateId() {
    return reversalTransactionId;
  }
}
