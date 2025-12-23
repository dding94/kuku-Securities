# Week 4: 원장 시스템 검증 테스트 전략

## 목표
PENDING/UNKNOWN 중간 상태를 구현하고, 동시성 테스트, 대량 데이터 처리 성능 테스트, 장애 시나리오 테스트를 통해 원장 시스템의 **Resilience(회복 탄력성)**를 검증한다.

---

## PR 1: TransactionStatus UNKNOWN 상태 추가 (~150 LOC)

> **목표.md 반영**: PENDING/UNKNOWN 중간 상태 정의 및 구현

### TDD Cycle
- [x] **RED**: TransactionStatus.UNKNOWN 테스트 작성
  - UNKNOWN 상태 생성 가능 여부 테스트
  - UNKNOWN → POSTED 전환 가능 여부 테스트
  - UNKNOWN → REVERSED 전환 불가 테스트
- [x] **GREEN**: TransactionStatus에 UNKNOWN 추가 및 상태 전이 규칙 구현
- [x] **REFACTOR**: 상태 전이 로직을 Transaction 엔티티로 캡슐화

### 구현 항목
- [x] `TransactionStatus.UNKNOWN` enum 값 추가
- [x] Unknown 상태 감지 기준 정의
  - Timeout (설정 가능한 임계값)
  - 외부 시스템 Exception
  - DB 커넥션 실패
- [x] `Transaction.markAsUnknown()` 메서드 구현
- [x] `Transaction.resolveUnknown(TransactionStatus)` 메서드 구현
- [x] schema.sql 업데이트 (UNKNOWN 상태 설명 주석)

### 문서화
- [x] README.md 업데이트 (UNKNOWN 상태 설명 추가)

- [x] PR 생성 및 머지

---

## PR 2: PENDING → POSTED 2단계 전환 로직 (~200 LOC)

> **목표.md 반영**: PENDING → POSTED 2단계 전환 로직 구현

### TDD Cycle
- [x] **RED**: 2단계 전환 성공 테스트 작성
  - Given: PENDING 상태의 Transaction
  - When: 외부 확인 완료 후 confirm 호출
  - Then: Transaction → POSTED, JournalEntry 적용, Balance 반영
- [x] **GREEN**: `ConfirmTransactionUseCase` 구현
- [x] **REFACTOR**: 기존 입금/출금 로직과 통합

### TDD Cycle (실패 케이스)
- [x] **RED**: PENDING 상태가 아닌 Transaction confirm 실패 테스트
- [x] **GREEN**: 상태 검증 로직 추가
- [x] **REFACTOR**: 예외 처리 일관성 확보

### 구현 항목
- [x] `ConfirmTransactionUseCase` 인터페이스 생성
- [x] `ConfirmTransactionCommand` 생성
- [x] `ConfirmTransactionService` 구현
- [ ] 기존 `DepositService`, `WithdrawService`에 PENDING 모드 옵션 추가
  > **Week 7 (Matching Engine 연동 시) 구현 예정** - 외부 체결 결과 대기 시나리오에서 필요

### Hexagonal Architecture 확장
```
application/
├── port/in/
│   ├── ConfirmTransactionUseCase
│   └── command/
│       └── ConfirmTransactionCommand
└── service/
    └── ConfirmTransactionService
```

- [x] PR 생성 및 머지

---

## PR 3: 동시성 테스트 강화 (~250 LOC)

> **목표.md 반영**: 동시성 테스트 (동일 계좌 동시 입출금 시 정합성 검증)

### TDD Cycle
- [x] **RED**: 동일 계좌 동시 입금 테스트
  - Given: 잔액 0원, 10개 스레드가 각각 100원 입금
  - Then: 최종 잔액 1000원
- [x] **GREEN**: 동시성 제어 적용/검증
- [x] **REFACTOR**: 테스트 가독성 개선

- [x] **RED**: 동일 계좌 동시 입출금 혼합 테스트
  - Given: 잔액 5000원, 5개 스레드 입금(100원), 5개 스레드 출금(100원)
  - Then: 최종 잔액 5000원
- [x] **GREEN**: Race Condition 없음 검증
- [x] **REFACTOR**: 테스트 유틸리티 추출

- [x] **RED**: Lost Update 방지 테스트
  - Given: 동일 계좌에 연속적인 업데이트
  - Then: 모든 업데이트가 반영됨
- [x] **GREEN**: Optimistic Lock 정상 동작 검증
- [x] **REFACTOR**: 정리

### 테스트 도구
- [x] `CountDownLatch`, `ExecutorService` 활용
- [x] 동시성 테스트 헬퍼 클래스 생성 (`ConcurrencyTestHelper`)

- [x] PR 생성 및 머지

---

## PR 4: Optimistic Lock 실패 처리 전략 (~200 LOC)

> **목표.md 반영**: Optimistic Lock 실패 처리 전략 구현

### TDD Cycle
- [x] **RED**: OptimisticLockException 발생 시 재시도 테스트
  - Given: 동시 업데이트로 인한 충돌 발생
  - When: 재시도 로직 실행
  - Then: 최대 N회 재시도 후 성공 또는 최종 실패
- [x] **GREEN**: `@Retryable` 또는 `RetryTemplate` 적용
- [x] **REFACTOR**: 재시도 설정 외부화 (application.yml) - 필요시 적용 예정

### 구현 항목
- [x] Spring Retry 의존성 추가 (build.gradle)
- [x] 재시도 로직 구현
  - 최대 재시도 횟수: 3회
  - 재시도 간격: Exponential Backoff (100ms, 200ms, 400ms)
  - 재시도 대상 예외: `OptimisticLockingFailureException`
- [ ] 클라이언트 에러 응답 표준화
  - 409 Conflict: 재시도 실패 시
  - 에러 응답 DTO 정의

### 문서화
- [x] 재시도 전략 ADR 작성 (`/docs/adr/007-retry-strategy.md`)

- [x] PR 생성 및 머지

---

## PR 5: 대량 데이터 처리 성능 테스트 (~200 LOC)

> **목표.md 반영**: 대량 데이터 처리 성능 테스트

### 테스트 시나리오
- [ ] 10,000건 Transaction 일괄 생성 성능 측정
  - Batch Insert vs 개별 Insert 비교
  - 목표: 1초 이내 완료
- [ ] 100,000건 JournalEntry 조회 성능 측정
  - 페이징 쿼리 성능 검증
  - 인덱스 효율성 확인
- [ ] 동일 계좌 1,000건 연속 트랜잭션 처리 성능
  - Lock 경합 영향도 측정
  - TPS(Transaction Per Second) 측정

### 구현 항목
- [ ] 성능 테스트 클래스 생성 (`LedgerPerformanceTest`)
- [ ] 테스트 결과 로깅 및 리포트 생성
- [ ] 병목 구간 식별 및 최적화 포인트 문서화

### 인덱스 검토
- [ ] 기존 인덱스 효율성 검토
- [ ] 필요시 추가 인덱스 생성 (schema.sql 업데이트)

- [ ] PR 생성 및 머지

---

## PR 6: Outbox 패턴 기반 이벤트 설계 (~250 LOC)

> **목표.md 반영**: Ledger 이벤트(Outbox + Kafka) 설계

### 도메인 이벤트 정의
- [ ] `LedgerEvent` 인터페이스 정의
- [ ] `LedgerPostedEvent` 구현
  - transactionId
  - accountId
  - amount
  - transactionType
  - timestamp
- [ ] `LedgerReversedEvent` 구현

### Outbox 테이블 설계
- [ ] `outbox_event` 테이블 스키마 설계
  ```sql
  CREATE TABLE outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    INDEX idx_outbox_status (status),
    INDEX idx_outbox_created (created_at)
  );
  ```
- [ ] schema.sql 업데이트

### Outbox Port 정의
- [ ] `OutboxEventPort` 인터페이스 생성
  - save(OutboxEvent)
  - findPendingEvents(limit)
  - markAsProcessed(eventId)

### 서비스 통합
- [ ] 기존 UseCase에 Outbox 이벤트 저장 로직 추가
  - DepositService: LedgerPostedEvent 저장
  - WithdrawService: LedgerPostedEvent 저장
  - ReversalService: LedgerReversedEvent 저장

### 문서화
- [ ] **[Diagram]** Ledger 이벤트 흐름도 작성 (`/docs/diagrams/ledger-event-flow.md`)

- [ ] PR 생성 및 머지

---

## PR 7: 장애 시나리오 테스트 - DB Lock 경쟁 (~200 LOC)

> **목표.md 반영**: DB Lock 경쟁 시나리오 테스트

### 테스트 시나리오
- [ ] **RED**: Deadlock 발생 시나리오 테스트
  - Given: 두 트랜잭션이 서로 다른 순서로 락 획득 시도
  - Then: Deadlock 감지 및 적절한 예외 처리
- [ ] **GREEN**: Deadlock 방지 전략 구현
- [ ] **REFACTOR**: 락 획득 순서 일관성 보장

- [ ] **RED**: Lock Timeout 시나리오 테스트
  - Given: 락 획득 대기 시간 초과
  - Then: 적절한 예외 발생 및 UNKNOWN 상태 전환
- [ ] **GREEN**: Lock Timeout 설정 및 예외 처리
- [ ] **REFACTOR**: Timeout 설정 외부화

### 구현 항목
- [ ] Lock Timeout 설정 (application.yml)
- [ ] Deadlock 감지 시 예외 처리 (`LockConflictException`)
- [ ] 락 관련 메트릭 로깅

- [ ] PR 생성 및 머지

---

## PR 8: 장애 시나리오 테스트 - Kafka 다운 시 Outbox 검증 (~250 LOC)

> **목표.md 반영**: Kafka 다운 시 Outbox 패턴 검증

### 테스트 시나리오
- [ ] **RED**: Kafka 다운 상태에서 트랜잭션 정상 완료 테스트
  - Given: Kafka 연결 불가 상태
  - When: 입금 트랜잭션 수행
  - Then: Transaction POSTED, Outbox에 이벤트 저장, Kafka 발행은 보류
- [ ] **GREEN**: Outbox 패턴 정상 동작 구현
- [ ] **REFACTOR**: 분리 및 정리

- [ ] **RED**: Kafka 복구 후 Outbox 이벤트 발행 테스트
  - Given: Outbox에 PENDING 이벤트 존재
  - When: Kafka 복구 후 폴링 실행
  - Then: 모든 PENDING 이벤트 발행 및 PROCESSED로 전환
- [ ] **GREEN**: Outbox 폴링 로직 구현
- [ ] **REFACTOR**: 폴링 주기 및 배치 크기 최적화

### 구현 항목
- [ ] `OutboxEventPublisher` 스케줄링 구현
  - **방식**: Spring `@Scheduled` 사용 (비동기 처리 불필요, 단순 주기적 실행)
  - **설정** (application.yml 에 외부화):
    - `kuku.ledger.outbox.polling-interval-ms`: 2000 (2초)
    - `kuku.ledger.outbox.batch-size`: 100
  - **실패 처리**:
    - Kafka 발행 실패 시 `retry_count` 증가 및 다음 폴링 때 재시도.
    - `max_retries`(예: 5회) 초과 시 `status = FAILED` 로 변경하여 무한 루프 방지.
    - Dead Letter Queue (DLQ) 개념을 DB 테이블 내 상태(`FAILED`)로 대체.
- [ ] Kafka Producer 구현 (기본 구조)
- [ ] 테스트용 Kafka Mock 또는 Testcontainers 활용

### 문서화
- [ ] Outbox 패턴 ADR 작성 (`/docs/adr/007-outbox-pattern.md`)

- [ ] PR 생성 및 머지

---

## PR 9: C4 Component Diagram 및 문서화 (~100 LOC)

> **목표.md 반영**: C4 Component Diagram (Ledger 내부 구조) 작성

### 다이어그램 작성
- [ ] **[Diagram]** C4 Component Diagram 작성 (`/docs/diagrams/c4-component-ledger.md`)
  - Domain Layer 컴포넌트
  - Application Layer 컴포넌트 (UseCases)
  - Adapter Layer 컴포넌트 (Web, Persistence)
  - 외부 시스템 연결 (Kafka, MySQL)

### 문서 정리
- [ ] Week 4 작업 내용 README.md 반영
- [ ] 기존 다이어그램과의 일관성 검토

### 회고
- [ ] Week 4 회고 작성 (`/docs/retrospectives/week-04.md`)
  - 🎯 이번 주 목표 달성도
  - ✅ 잘한 점 (Keep)
  - 🔧 개선할 점 (Problem)
  - 💡 시도해볼 것 (Try)
  - 📝 배운 점 / 기술 인사이트

- [ ] PR 생성 및 머지

---

## Hexagonal Architecture 패키지 구조 (Week 4 확장)

> Week 3 구조에서 확장된 부분을 표시 (**NEW**)

```
kuku-core-ledger/src/main/java/com/securities/kuku/ledger/
├── domain/
│   ├── Account, Transaction, JournalEntry, Balance
│   ├── AccountType, TransactionType, TransactionStatus
│   ├── InvalidTransactionStateException, InsufficientBalanceException
│   ├── LockConflictException                           # **NEW**
│   └── event/                                           # **NEW**
│       ├── LedgerEvent
│       ├── LedgerPostedEvent
│       └── LedgerReversedEvent
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── DepositUseCase, WithdrawUseCase, ReversalUseCase
│   │   │   ├── ConfirmTransactionUseCase               # **NEW**
│   │   │   └── command/
│   │   │       ├── DepositCommand, WithdrawCommand, ReversalCommand
│   │   │       └── ConfirmTransactionCommand           # **NEW**
│   │   └── out/
│   │       ├── TransactionPort, AccountPort, BalancePort, JournalEntryPort
│   │       └── OutboxEventPort                         # **NEW**
│   └── service/
│       ├── DepositService, WithdrawService, ReversalService
│       └── ConfirmTransactionService                   # **NEW**
├── adapter/
│   ├── in/web/
│   │   └── LedgerController (향후)
│   └── out/
│       ├── persistence/
│       │   ├── JpaTransactionAdapter
│       │   ├── JpaAccountAdapter
│       │   ├── JpaBalanceAdapter
│       │   ├── JpaJournalEntryAdapter
│       │   └── JpaOutboxEventAdapter                   # **NEW**
│       └── messaging/
│           └── KafkaOutboxPublisher                    # **NEW**
└── config/
    └── RetryConfig                                     # **NEW**
```

---

## 테스트 파일 구조 (Week 4 확장)

```
kuku-core-ledger/src/test/java/com/securities/kuku/ledger/
├── domain/
│   ├── AccountTest, TransactionTest, JournalEntryTest, BalanceTest
│   ├── TransactionStatusTest
│   └── event/                                          # **NEW**
│       └── LedgerEventTest
├── application/service/
│   ├── DepositServiceTest, WithdrawServiceTest, ReversalServiceTest
│   ├── ConfirmTransactionServiceTest                   # **NEW**
│   └── OutboxEventPublisherTest                        # **NEW**
├── concurrency/                                         # **NEW**
│   └── LedgerConcurrencyTest
├── performance/                                         # **NEW**
│   └── LedgerPerformanceTest
└── resilience/                                          # **NEW**
    ├── DbLockScenarioTest
    └── KafkaDownScenarioTest
```

---

## 주요 기술 결정 사항

### 1. UNKNOWN 상태 감지 기준

| 상황 | 결과 상태 | 처리 방법 |
|------|----------|----------|
| DB 커밋 성공 + Outbox 저장 성공 | POSTED | 정상 |
| DB 커밋 실패 | 트랜잭션 롤백 | 재시도 |
| DB 커밋 성공 + Outbox Timeout | UNKNOWN | 수동 확인 필요 |
| 외부 시스템 응답 Timeout | UNKNOWN | 나중에 동기화 |

### 2. 재시도 전략

| 설정 | 값 |
|------|-----|
| 최대 재시도 횟수 | 3 |
| 초기 대기 시간 | 100ms |
| Backoff Multiplier | 2.0 |
| 최대 대기 시간 | 1000ms |
| 재시도 대상 예외 | OptimisticLockingFailureException |

### 3. Outbox 폴링 설정

| 설정 | 값 |
|------|-----|
| 폴링 주기 | 5초 |
| 배치 크기 | 100건 |
| 실패 시 재시도 | 3회 |
| 최대 보관 기간 | 7일 |

---

## Week 5 Preview

Week 4 완료 후 Order System 구현으로 넘어갑니다:

- [ ] 주문 상태 머신(State Machine) 설계
- [ ] CREATED → VALIDATED → FILLED / REJECTED / CANCELLED
- [ ] 상태 패턴(State Pattern) 적용
- [ ] 예수금 부족, 보유 수량 부족 시 REJECT 구현

