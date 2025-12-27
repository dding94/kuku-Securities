package com.securities.kuku.ledger.application.port.out;

import com.securities.kuku.ledger.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;

/** Outbox 이벤트 저장 및 조회를 위한 Outbound Port. */
public interface OutboxEventPort {

  /**
   * Outbox 이벤트를 저장합니다.
   *
   * @param event 저장할 이벤트
   * @return 저장된 이벤트 (ID 포함)
   */
  OutboxEvent save(OutboxEvent event);

  /**
   * PENDING 상태의 이벤트 목록을 조회합니다. (PR 8에서 구현)
   *
   * @param limit 최대 조회 개수
   * @return PENDING 상태의 이벤트 목록
   */
  List<OutboxEvent> findPendingEvents(int limit);

  /**
   * 이벤트를 PROCESSED 상태로 변경합니다. (PR 8에서 구현)
   *
   * @param eventId 이벤트 ID
   * @param processedAt 처리 시각
   */
  void markAsProcessed(Long eventId, Instant processedAt);

  /**
   * 이벤트를 FAILED 상태로 변경합니다. (PR 8에서 구현)
   *
   * @param eventId 이벤트 ID
   * @param retryCount 재시도 횟수
   */
  void markAsFailed(Long eventId, int retryCount);
}
