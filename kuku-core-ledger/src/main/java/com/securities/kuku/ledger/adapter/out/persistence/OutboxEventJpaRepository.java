package com.securities.kuku.ledger.adapter.out.persistence;

import com.securities.kuku.ledger.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.securities.kuku.ledger.domain.OutboxEventStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {

  /**
   * 특정 상태의 이벤트를 생성 시간 순으로 조회합니다. (PR 8에서 사용)
   *
   * @param status 조회할 상태
   * @param pageable 페이징 정보
   * @return 이벤트 목록
   */
  List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(
      OutboxEventStatus status, Pageable pageable);
}
