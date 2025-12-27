package com.securities.kuku.ledger.adapter.out.persistence.entity;

import com.securities.kuku.ledger.domain.OutboxEvent;
import com.securities.kuku.ledger.domain.OutboxEventStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEventJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "payload", columnDefinition = "JSON", nullable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OutboxEventStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  public OutboxEventJpaEntity(
      Long id,
      String aggregateType,
      Long aggregateId,
      String eventType,
      String payload,
      OutboxEventStatus status,
      int retryCount,
      Instant createdAt,
      Instant processedAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.status = status;
    this.retryCount = retryCount;
    this.createdAt = createdAt;
    this.processedAt = processedAt;
  }

  public OutboxEvent toDomain() {
    return OutboxEvent.restore(
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

  public static OutboxEventJpaEntity fromDomain(OutboxEvent event) {
    return new OutboxEventJpaEntity(
        event.getId(),
        event.getAggregateType(),
        event.getAggregateId(),
        event.getEventType(),
        event.getPayload(),
        event.getStatus(),
        event.getRetryCount(),
        event.getCreatedAt(),
        event.getProcessedAt());
  }
}
