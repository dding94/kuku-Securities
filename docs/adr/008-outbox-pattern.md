# [ADR-008] Outbox 패턴을 통한 이벤트 발행 원자성 보장

*   **Status**: Accepted
*   **Date**: 2025-12-26
*   **Author**: dding94

## 1. Context (배경)

### 1.1. 문제 상황

금융 원장 시스템에서 **트랜잭션 완료 이벤트**를 외부 시스템(Kafka)으로 발행해야 합니다.

```
┌─────────────────────────────────────────────────────────────┐
│  DepositService.deposit()                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 1. Transaction 저장                                      ││
│  │ 2. JournalEntry 저장                                     ││
│  │ 3. Balance 갱신                                          ││
│  │ 4. DB Commit ✅                                          ││
│  └─────────────────────────────────────────────────────────┘│
│  ↓                                                          │
│  5. Kafka 발행 ← 이 시점에서 실패하면?                      │
└─────────────────────────────────────────────────────────────┘
```

**Dual Write 문제:**
- DB Commit 성공 후 Kafka 발행 실패 시 → 데이터 불일치
- Kafka 발행 성공 후 DB Commit 실패 시 → 유령 이벤트 발행

### 1.2. 금융 도메인의 특수성

| 일반 서비스 | 금융 원장 시스템 |
|------------|------------------|
| 이벤트 유실 허용 가능 | **이벤트 유실 = 규제 위반** |
| 중복 발행 무관 | 중복 발행 = 이중 정산 위험 |
| 느슨한 일관성 허용 | **강한 감사 추적 필수** |

**규제 요구사항:**
- **전자금융감독규정 제23조 (거래기록의 보존)**: 모든 금융 거래는 5년간 추적 가능해야 함
- **전자금융거래법 제22조**: 원장 변경과 이벤트 발행의 정합성 보장 필수

### 1.3. 왜 Outbox 패턴이 필요한가?

**이벤트 발행이 필요한 이유:**
1. **CQRS Read Model 갱신**: 포트폴리오 뷰, 잔액 조회 서비스
2. **후속 처리 트리거**: 주문 시스템, 포지션 서비스
3. **감사 로그**: 외부 감사 시스템 연동

**단순 Kafka 발행의 문제점:**

```java
// ❌ Anti-Pattern: Dual Write
@Transactional
public void deposit(DepositCommand command) {
    transactionPort.save(transaction);
    balancePort.update(balance);
    // DB Commit 시점 ← 여기까지 성공
    
    kafkaTemplate.send("ledger-events", event); // ← 실패 시 불일치!
}
```

| 실패 시나리오 | DB 상태 | Kafka 상태 | 결과 |
|--------------|---------|------------|------|
| Kafka 네트워크 오류 | 커밋됨 ✅ | 발행 안 됨 ❌ | **이벤트 유실** |
| Kafka 응답 지연 > Timeout | 커밋됨 ✅ | 발행됨(?) | **상태 불명** |
| DB Commit 실패 | 롤백 ❌ | 발행됨 ✅ | **유령 이벤트** |

## 2. Decision (결정)

**Outbox 패턴**을 적용하여 이벤트 발행의 원자성을 보장한다.

### 핵심 원칙

> **"이벤트 저장과 비즈니스 로직을 동일 DB 트랜잭션에서 처리한다"**

```
┌─────────────────────────────────────────────────────────────┐
│  Single DB Transaction                                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 1. Transaction 저장                                      ││
│  │ 2. JournalEntry 저장                                     ││
│  │ 3. Balance 갱신                                          ││
│  │ 4. OutboxEvent 저장 ← 동일 트랜잭션!                     ││
│  │ 5. DB Commit (All or Nothing)                            ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
│  비동기 Polller (별도 프로세스)                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 6. OutboxEvent 조회 (PENDING)                            ││
│  │ 7. Kafka 발행                                            ││
│  │ 8. OutboxEvent 상태 변경 (PROCESSED)                     ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 메시지 전달 보장 수준

> [!IMPORTANT]
> Outbox 패턴은 **At-Least-Once 전달**을 보장합니다. Exactly-Once가 아닙니다.

**발생 가능 시나리오:**
1. Kafka 발행 성공 → PROCESSED 업데이트 전 서버 다운 → 재시작 후 재발행 → **중복 발행**
2. 네트워크 지연으로 Kafka ACK 미수신 → 재시도 → **중복 발행**

**컨슈머 측 Idempotency 필수:**
```java
// 컨슈머 측 중복 방어 (PR 8에서 상세 설계)
@KafkaListener(topics = "ledger-events")
public void handleLedgerEvent(LedgerPostedEvent event) {
    // Idempotency Key = transactionId
    if (processedEventRepository.existsByTransactionId(event.transactionId())) {
        log.warn("Duplicate event ignored: {}", event.transactionId());
        return;
    }
    // 실제 처리...
}
```

### 구현 범위

**PR 6 (현재):** Outbox 저장 인프라
- 도메인 이벤트: `LedgerPostedEvent`, `LedgerReversedEvent`
- Outbox 엔티티 및 Port
- 서비스 통합 (`OutboxEventRecorder`)

**PR 8 (향후):** Kafka 발행 인프라
- `OutboxEventPublisher` 스케줄링
- Kafka Producer
- 실패 처리 (재시도, FAILED 상태)

## 3. Alternatives Considered (대안 분석)

### 3.1. Outbox 패턴 ✅ (선택)

```java
@Transactional
public void deposit(DepositCommand command) {
    transactionPort.save(transaction);
    balancePort.update(balance);
    outboxEventRecorder.record(LedgerPostedEvent.of(...)); // 동일 트랜잭션
}
```

| 장점 | 단점 |
|------|------|
| DB 트랜잭션으로 원자성 보장 | Polling 지연 (2~5초) |
| Kafka 다운 시에도 이벤트 보존 | 추가 테이블 필요 |
| 재발행 가능 (PENDING 상태) | 폴링 로직 구현 필요 |
| 감사 추적 용이 | At-Least-Once (중복 가능) |

### 3.2. Transactional Outbox with CDC (Change Data Capture) ❌

Debezium 등을 사용하여 Outbox 테이블 변경을 자동 캡처:

| 장점 | 단점 |
|------|------|
| Near Real-time 전송 (~100ms) | 인프라 복잡도 증가 (Debezium, Kafka Connect) |
| Polling 불필요 | 운영 부담 증가 |
| | 초기 셋업 비용 |

> **현재 단계에서는 단순한 Polling 방식**을 선택합니다. 
> **CDC 마이그레이션 기준:** 이벤트 발행량이 **초당 1,000건 이상**이거나 **지연 시간 요구가 500ms 이하**인 경우 검토.

### 3.3. 2PC (Two-Phase Commit) ❌

XA 트랜잭션으로 DB + Kafka를 묶음:

| 장점 | 단점 |
|------|------|
| 강한 일관성 | 성능 저하 심각 |
| 표준 기술 | Kafka는 XA 미지원 |
| | 분산 락 구현 복잡 |

> **Kafka는 XA 트랜잭션을 지원하지 않으므로 적용 불가.**

### 3.4. Saga 패턴 (Choreography) ❌

이벤트 기반 보상 트랜잭션:

| 장점 | 단점 |
|------|------|
| 느슨한 결합 | 복잡한 보상 로직 |
| 확장성 | 디버깅 어려움 |
| | 상태 추적 어려움 |

> **원장 시스템은 이미 단일 서비스 내 트랜잭션**이므로 Saga 불필요.

### 3.5. @TransactionalEventListener ❌

Spring의 이벤트 리스너 활용:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleLedgerPosted(LedgerPostedEvent event) {
    kafkaTemplate.send(...);
}
```

| 장점 | 단점 |
|------|------|
| 코드 간결 | 리스너 실패 시 이벤트 유실 |
| Spring 네이티브 | 재시도 메커니즘 없음 |
| | 서버 재시작 시 미발행 이벤트 손실 |

> **Outbox 패턴은 DB에 이벤트가 영속화**되어 서버 재시작에도 안전합니다.

## 4. Implementation Details (구현 상세)

### 4.1. Outbox 테이블 스키마

```sql
CREATE TABLE outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,     -- "TRANSACTION"
    aggregate_id BIGINT NOT NULL,            -- transactionId
    event_type VARCHAR(100) NOT NULL,        -- "LEDGER_POSTED"
    payload JSON NOT NULL,                   -- 직렬화된 이벤트
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    INDEX idx_outbox_status_created (status, created_at)
);
```

**인덱스 설계:**
- `idx_outbox_status_created`: Polling 시 `WHERE status = 'PENDING' ORDER BY created_at` 최적화

### 4.2. 상태 전이

```
[*] ─────────────→ PENDING ─────────────→ PROCESSED
                      │                        ↑
                      │ 발행 실패              │ 발행 성공
                      │ (retry < max)          │
                      └────────────────────────┘
                      │
                      │ retry >= max
                      ↓
                   FAILED ─────────→ 수동 조치
```

### 4.3. Polling 주기 설정 근거

| 설정 | 값 | 선택 근거 |
|------|-----|----------|
| `polling-interval` | 2초 | 금융 도메인 특성상 실시간성보다 정확성 우선. Read Model 갱신 지연 2~3초 허용 |
| `batch-size` | 100건 | DB 조회 1회당 처리량과 메모리 사용량의 균형점 |
| `max-retry` | 5회 | Kafka 일시 장애(수 분) 대응. 5회 × 2초 = 최대 10초 재시도 |

**왜 2초인가?**
- 100ms~500ms: Read Model 갱신에 과도한 실시간성 (불필요)
- 2초: PENDING 이벤트 적재량 최소화 + DB 부하 감소
- 10초 이상: 사용자 체감 지연 발생 (잔액 조회 불일치)

### 4.4. Idempotency 보장 (@Retryable과의 관계)

```
@Retryable (Optimistic Lock 재시도)
  └─ @Transactional
       ├─ 비즈니스 로직
       └─ OutboxEvent 저장
              └─ 재시도 시 전체 롤백 → OutboxEvent도 롤백
                   → 중복 저장 없음
```

**핵심:** 재시도 시 트랜잭션 전체가 롤백되므로 Outbox 이벤트도 함께 롤백됩니다.

### 4.5. 패키지 구조

```
domain/
├── event/
│   ├── LedgerEvent (interface)
│   ├── LedgerPostedEvent
│   └── LedgerReversedEvent
├── OutboxEvent
└── OutboxEventStatus

application/
├── port/out/OutboxEventPort
└── service/OutboxEventRecorder

adapter/out/persistence/
├── OutboxEventJpaEntity
├── OutboxEventJpaRepository
└── OutboxEventPersistenceAdapter
```

### 4.6. Schema Evolution Strategy (스키마 변경 대응)

이벤트 구조(Payload) 변경 시, 이미 저장된 이벤트(Pending/Failed)와의 호환성을 보장하기 위해 다음 원칙을 따릅니다.

1.  **Additive Changes Only (기본 원칙)**:
    *   새로운 필드 추가는 허용하되, 필수(Required) 필드가 아닌 **Optional** 필드로 추가합니다.
    *   기존 필드의 삭제나 타입 변경은 원칙적으로 금지합니다. (Breaking Change 방지)
2.  **Unknown Properties Ignore**:
    *   `ObjectMapper` 설정에서 `FAIL_ON_UNKNOWN_PROPERTIES = false`를 적용합니다.
    *   이를 통해 상위 버전 이벤트가 하위 버전 코드로 읽힐 때 발생하는 오류를 방지합니다 (Rolling Update 지원).
3.  **Upcasting (Advanced)**:
    *   초기에는 적용하지 않으나, 향후 스키마 변경이 급격해질 경우 Consumer Side Upcasting 패턴 도입을 검토합니다.

## 5. Consequences (결과)

### 5.1. 기대 효과

| 측면 | 변화 |
|------|------|
| **원자성** | DB 트랜잭션으로 비즈니스 로직과 이벤트 저장 동시 보장 |
| **신뢰성** | Kafka 다운 시에도 이벤트 보존, 복구 후 재발행 |
| **감사 추적** | Outbox 테이블이 이벤트 히스토리 역할 |
| **디커플링** | Kafka 의존성이 서비스 레이어에서 분리 |

### 5.2. 트레이드오프

| 비용 | 설명 |
|------|------|
| **Latency** | Polling 주기만큼 이벤트 발행 지연 (2~5초) |
| **Storage** | Outbox 테이블 공간 (processed 이벤트 주기적 정리 필요) |
| **Complexity** | Polling 로직, 상태 관리 코드 추가 |
| **At-Least-Once** | 컨슈머 측 Idempotency 구현 필수 |

### 5.3. 운영 고려사항

**1. 오래된 이벤트 정리 (Retention Policy):**
```sql
-- 7일 이상 지난 PROCESSED 이벤트 삭제 (배치)
DELETE FROM outbox_event 
WHERE status = 'PROCESSED' 
  AND processed_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
```
*   **정리 이유:**
    1.  **성능 유지**: Outbox 테이블은 빈번한 Insert/Sort가 발생하므로, 데이터가 비대해지면 인덱스 오버헤드(B-Tree 깊이 증가)로 전체 트랜잭션 성능이 저하됩니다.
    2.  **역할 구분**: Outbox는 "영구 보관소"가 아닌 "발행 버퍼"입니다. 장기 보관 및 감사(Audit) 데이터는 별도의 Cold Storage나 Kafka Retention 정책으로 관리합니다.
    3.  **비용 효율**: 완료된 이벤트를 고비용의 트랜잭션 DB(SSD)에 보관하는 것은 낭비입니다.

**2. FAILED 이벤트 알림:**
- `status = 'FAILED'` 이벤트 발생 시 Slack 알림
- 수동 재처리 또는 근본 원인 분석

**3. 장시간 PENDING 이벤트 감지:**
```sql
-- 5분 이상 PENDING 상태인 이벤트 (Kafka 장애 의심)
SELECT COUNT(*) FROM outbox_event 
WHERE status = 'PENDING' 
  AND created_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE);
```
- **임계값 초과 시 알림**: 5분 이상 PENDING 100건 이상 → PagerDuty 알람
- **대응책**: Kafka 클러스터 상태 확인, 수동 재시작

**4. 모니터링 지표:**
- PENDING 이벤트 수 (지연 감지)
- 평균 처리 시간 (created_at → processed_at)
- FAILED 이벤트 수 (알람 임계값)

## 6. Future Considerations (후속 고려사항 - PR 8)

> [!NOTE]
> 아래 항목들은 PR 8 (Kafka 발행 인프라)에서 상세 설계 예정입니다.

### 6.1. 이벤트 순서 보장

**문제:** 동일 계좌에서 입금 → 출금 순서로 발생 시, Polling 타이밍에 따라 Kafka 발행 순서가 뒤바뀔 수 있음.

**해결 방향:**
- `aggregate_id`(accountId) 기준 **파티셔닝**: 동일 계좌 이벤트는 동일 파티션으로
- Single Consumer per Partition: 순서 보장
- 또는 이벤트에 `sequence_number` 추가 → 컨슈머에서 순서 검증

### 6.2. Kafka 토픽 설계

**옵션 A: 단일 토픽**
- `ledger-events` 토픽에 모든 이벤트
- 장점: 단순함
- 단점: 컨슈머가 불필요한 이벤트 필터링 필요

**옵션 B: 이벤트 타입별 분리**
- `ledger-posted-events`, `ledger-reversed-events`
- 장점: 컨슈머 선택적 구독
- 단점: 토픽 관리 복잡

### 6.3. 테이블 파티셔닝 전략 (대용량 대비)

**현재:** 단일 테이블 + 배치 삭제
**향후:** 월별 파티셔닝 또는 Archive 테이블 분리 검토

```sql
-- 예시: 월별 파티셔닝
ALTER TABLE outbox_event PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at));
```

## 7. References

*   [Microservices.io - Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
*   [Chris Richardson - Saga Pattern](https://microservices.io/patterns/data/saga.html)
*   [Debezium CDC](https://debezium.io/documentation/reference/stable/connectors/mysql.html)
*   [Martin Fowler - Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
*   전자금융감독규정 제23조 (거래기록의 보존)
*   `/docs/diagrams/ledger-event-flow.md` - 이벤트 흐름도
