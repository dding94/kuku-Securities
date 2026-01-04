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

## ~~PR 5: 대량 데이터 처리 성능 테스트~~ → Week 12로 이관

> [!NOTE]
> **이관 사유**: 성능 테스트는 전체 시스템(주문→체결→원장→포지션)이 완성된 후 의미 있는 측정이 가능함.
>
> **Week 12 계획**:
> - k6/nGrinder를 활용한 부하 테스트 (대량 데이터 생성/조회, 동시 요청 시나리오 포함)
> - 성능 지표 측정 (TPS, Latency, P99)
> - 병목 구간 분석 및 인덱스 최적화
>
> **Week 4 Resilience 테마에서 이미 검증된 항목**:
> - ✅ 동시성 정합성 → PR 3 `LedgerConcurrencyTest`
> - ✅ Optimistic Lock 재시도 → PR 4 `RetryIntegrationTest`

---

## PR 6: Outbox 패턴 기반 이벤트 설계 (~250 LOC)

> **목표.md 반영**: Ledger 이벤트(Outbox + Kafka) 설계

### 도메인 이벤트 정의
- [x] `LedgerEvent` 인터페이스 정의
- [x] `LedgerPostedEvent` 구현
  - transactionId
  - accountId
  - amount
  - transactionType
  - timestamp
- [x] `LedgerReversedEvent` 구현

### Outbox 테이블 설계
- [x] `outbox_event` 테이블 스키마 설계
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
- [x] schema.sql 업데이트

### Outbox Port 정의
- [x] `OutboxEventPort` 인터페이스 생성
  - save(OutboxEvent)
  - findPendingEvents(limit)
  - markAsProcessed(eventId)

### 서비스 통합
- [x] 기존 UseCase에 Outbox 이벤트 저장 로직 추가
  - DepositService: LedgerPostedEvent 저장
  - WithdrawService: LedgerPostedEvent 저장
  - ReversalService: LedgerReversedEvent 저장

### 문서화
- [x] **[Diagram]** Ledger 이벤트 흐름도 작성 (`/docs/diagrams/ledger-event-flow.md`)

- [x] PR 생성 및 머지

---

## ~~PR 7: 장애 시나리오 테스트 - DB Lock 경쟁~~ → Week 6으로 이관

> [!NOTE]
> **이관 사유**: Week 6 "동시성 제어 전략" 주차에서 극한 동시성 테스트와 함께 통합 실험 예정.
>
> **Week 6 계획**:
> - Deadlock 발생 시나리오 테스트
> - Lock Timeout 시나리오 테스트
> - Redis 장애 시 DB Lock만으로 방어 가능한지 검증
> - Optimistic Lock vs Pessimistic Lock vs Redis Distributed Lock 성능 비교

---

## ~~PR 8: 장애 시나리오 테스트 - Kafka 다운 시 Outbox 검증~~ → Week 7으로 이관

> [!NOTE]
> **이관 사유**: Kafka 연동은 Week 7에서 E2E 플로우(주문→체결→원장)와 함께 구현해야 의미 있음.
>
> **Week 7 계획**:
> - Outbox → Kafka Producer → Consumer 연결
> - LedgerPostedEvent 발행
> - Kafka 다운 시 Outbox 패턴 검증
> - Testcontainers (Kafka) 활용 통합 테스트

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

## Hexagonal Architecture 패키지 구조 (Week 4 최종)

> 실제 구현된 구조 (Week 4 완료 시점)

```
kuku-core-ledger/src/main/java/com/securities/kuku/ledger/
├── LedgerApplication.java
├── domain/
│   ├── Account.java
│   ├── AccountType.java
│   ├── Balance.java
│   ├── Transaction.java
│   ├── TransactionStatus.java
│   ├── TransactionType.java
│   ├── JournalEntry.java
│   ├── OutboxEvent.java
│   ├── OutboxEventStatus.java
│   ├── InsufficientBalanceException.java
│   ├── InvalidTransactionStateException.java
│   └── event/
│       ├── LedgerEvent.java
│       ├── LedgerPostedEvent.java
│       └── LedgerReversedEvent.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── DepositUseCase.java
│   │   │   ├── WithdrawUseCase.java
│   │   │   ├── ReversalUseCase.java
│   │   │   ├── ConfirmTransactionUseCase.java
│   │   │   └── command/
│   │   │       ├── DepositCommand.java
│   │   │       ├── WithdrawCommand.java
│   │   │       ├── ReversalCommand.java
│   │   │       └── ConfirmTransactionCommand.java
│   │   └── out/
│   │       ├── AccountPort.java
│   │       ├── BalancePort.java
│   │       ├── TransactionPort.java
│   │       ├── JournalEntryPort.java
│   │       └── OutboxEventPort.java
│   └── service/
│       ├── DepositService.java
│       ├── WithdrawService.java
│       ├── ReversalService.java
│       ├── ConfirmTransactionService.java
│       └── OutboxEventRecorder.java
├── adapter/
│   └── out/
│       └── persistence/
│           ├── AccountJpaRepository.java
│           ├── AccountPersistenceAdapter.java
│           ├── BalanceJpaRepository.java
│           ├── BalancePersistenceAdapter.java
│           ├── TransactionJpaRepository.java
│           ├── TransactionPersistenceAdapter.java
│           ├── JournalEntryJpaRepository.java
│           ├── JournalEntryPersistenceAdapter.java
│           ├── OutboxEventJpaRepository.java
│           ├── OutboxEventPersistenceAdapter.java
│           └── entity/
│               ├── AccountEntity.java
│               ├── BalanceEntity.java
│               ├── TransactionEntity.java
│               ├── JournalEntryEntity.java
│               └── OutboxEventEntity.java
└── config/
    ├── RetryConfig.java
    └── ClockConfig.java
```

> [!NOTE]
> **Week 7 구현 예정**: `adapter/in/web/` (LedgerController), `adapter/out/messaging/` (KafkaOutboxPublisher)

---

## 테스트 파일 구조 (Week 4 최종)

```
kuku-core-ledger/src/test/java/com/securities/kuku/ledger/
├── domain/
│   ├── AccountTest.java
│   ├── BalanceTest.java
│   ├── JournalEntryTest.java
│   ├── TransactionTest.java
│   ├── TransactionStatusTest.java
│   └── TransactionTypeTest.java
├── application/service/
│   ├── DepositServiceTest.java
│   ├── WithdrawServiceTest.java
│   ├── ReversalServiceTest.java
│   └── ConfirmTransactionServiceTest.java
├── concurrency/
│   └── LedgerConcurrencyTest.java
└── resilience/
    └── RetryIntegrationTest.java
```

> [!NOTE]
> **Week 6 구현 예정**: `DbLockScenarioTest` (Deadlock, Lock Timeout 시나리오)
> **Week 7 구현 예정**: `KafkaDownScenarioTest` (Outbox 검증)

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

