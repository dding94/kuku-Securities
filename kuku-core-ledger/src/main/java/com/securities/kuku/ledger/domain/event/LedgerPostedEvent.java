package com.securities.kuku.ledger.domain.event;

import com.securities.kuku.ledger.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerPostedEvent(
    Long transactionId,
    Long accountId,
    BigDecimal amount,
    TransactionType transactionType,
    Instant occurredAt)
    implements LedgerEvent {

  private static final String EVENT_TYPE = "LEDGER_POSTED";

  public static LedgerPostedEvent of(
      Long transactionId,
      Long accountId,
      BigDecimal amount,
      TransactionType transactionType,
      Instant occurredAt) {
    return new LedgerPostedEvent(transactionId, accountId, amount, transactionType, occurredAt);
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public Long aggregateId() {
    return transactionId;
  }
}
