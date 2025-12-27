package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.securities.kuku.ledger.application.port.out.OutboxEventPort;
import com.securities.kuku.ledger.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPersistenceAdapter implements OutboxEventPort {

  private final OutboxEventJpaRepository repository;

  @Override
  public OutboxEvent save(OutboxEvent event) {
    OutboxEventJpaEntity entity = OutboxEventJpaEntity.fromDomain(event);
    OutboxEventJpaEntity saved = repository.save(entity);
    return saved.toDomain();
  }

  @Override
  public List<OutboxEvent> findPendingEvents(int limit) {
    // PR 8에서 구현 예정
    throw new UnsupportedOperationException("Will be implemented in PR 8");
  }

  @Override
  public void markAsProcessed(Long eventId, Instant processedAt) {
    // PR 8에서 구현 예정
    throw new UnsupportedOperationException("Will be implemented in PR 8");
  }

  @Override
  public void markAsFailed(Long eventId, int retryCount) {
    // PR 8에서 구현 예정
    throw new UnsupportedOperationException("Will be implemented in PR 8");
  }
}
