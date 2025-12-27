package com.securities.kuku.ledger.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securities.kuku.ledger.application.port.out.OutboxEventPort;
import com.securities.kuku.ledger.domain.OutboxEvent;
import com.securities.kuku.ledger.domain.event.LedgerEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventRecorder {

  private static final String AGGREGATE_TYPE = "TRANSACTION";

  private final OutboxEventPort outboxEventPort;
  private final ObjectMapper objectMapper;

  public void record(LedgerEvent event) {
    String payload = serializeEvent(event);
    OutboxEvent outboxEvent =
        OutboxEvent.create(
            AGGREGATE_TYPE, event.aggregateId(), event.eventType(), payload, event.occurredAt());
    outboxEventPort.save(outboxEvent);
  }

  private String serializeEvent(LedgerEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize LedgerEvent: " + event.eventType(), e);
    }
  }
}
