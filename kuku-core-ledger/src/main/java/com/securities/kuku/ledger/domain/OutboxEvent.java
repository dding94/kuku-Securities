package com.securities.kuku.ledger.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OutboxEvent {

  private Long id;
  private String aggregateType;
  private Long aggregateId;
  private String eventType;
  private String payload;
  private OutboxEventStatus status;
  private int retryCount;
  private Instant createdAt;
  private Instant processedAt;

  public static OutboxEvent create(
      String aggregateType, Long aggregateId, String eventType, String payload, Instant createdAt) {
    return new OutboxEvent(
        null,
        aggregateType,
        aggregateId,
        eventType,
        payload,
        OutboxEventStatus.PENDING,
        0,
        createdAt,
        null);
  }

  public static OutboxEvent restore(
      Long id,
      String aggregateType,
      Long aggregateId,
      String eventType,
      String payload,
      OutboxEventStatus status,
      int retryCount,
      Instant createdAt,
      Instant processedAt) {
    return new OutboxEvent(
        id,
        aggregateType,
        aggregateId,
        eventType,
        payload,
        status,
        retryCount,
        createdAt,
        processedAt);
  }
}
