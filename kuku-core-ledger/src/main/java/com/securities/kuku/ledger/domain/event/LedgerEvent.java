package com.securities.kuku.ledger.domain.event;

import java.time.Instant;

/** 모든 Ledger 도메인 이벤트의 공통 인터페이스. Outbox 패턴을 통해 외부 시스템(Kafka)으로 발행됩니다. */
public interface LedgerEvent {

  /** 이벤트 타입 (예: "LEDGER_POSTED", "LEDGER_REVERSED") */
  String eventType();

  /** Aggregate ID (transactionId) */
  Long aggregateId();

  /** 이벤트 발생 시각 */
  Instant occurredAt();
}
