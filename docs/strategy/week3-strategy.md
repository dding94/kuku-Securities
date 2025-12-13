# Week 3: 원장 정합성 구현 전략

## 목표
입출금, 자산 이동 트랜잭션을 구현하고, **TransactionStatus + Reversal(역분개)** 패턴을 적용하여 금융 원장의 정합성을 보장한다.

---

## PR 1: TransactionStatus 및 도메인 확장 (~150 LOC)

### TDD Cycle
- [x] **RED**: TransactionStatus enum 테스트 작성
- [x] **GREEN**: TransactionStatus enum 구현 (PENDING, POSTED, REVERSED)
- [x] **REFACTOR**: 정리

- [x] **RED**: Transaction에 status 필드 테스트 작성
- [x] **GREEN**: Transaction 엔티티에 status, reversalOfTransactionId 필드 추가
- [x] **REFACTOR**: 기존 테스트와 통합

- [x] schema.sql 업데이트 (status, reversal_of_transaction_id 컬럼)
- [x] README.md 업데이트 (TransactionStatus 설명 추가)
- [x] PR 생성 및 머지

---

## PR 2: Hexagonal 포트 인터페이스 정의 (~100 LOC)

> **POLICY.md 반영**: 통합형 Port 권장 원칙에 따라 Aggregate별 단일 Port로 통합.

### Outbound Ports (UseCase → Repository)
- [x] `TransactionPort` 인터페이스 생성 (findById, findByBusinessRefId, save, update)
- [x] `AccountPort` 인터페이스 생성 (findById)
- [x] `BalancePort` 인터페이스 생성 (findByAccountId, findByAccountIds, update, updateAll)
- [x] `JournalEntryPort` 인터페이스 생성 (save, saveAll, findByTransactionId)

### Inbound Ports (Controller → UseCase)
- [x] `DepositUseCase` 인터페이스 생성
- [x] `WithdrawUseCase` 인터페이스 생성
- [x] `ReversalUseCase` 인터페이스 생성

- [x] PR 생성 및 머지

---

## PR 3: 입금(Deposit) UseCase 구현 (~200 LOC)

### TDD Cycle
- [x] **RED**: 입금 성공 테스트 작성
  - Given: 계좌, 입금액
  - When: deposit 호출
  - Then: Transaction(POSTED) 생성, JournalEntry(CREDIT) 생성, Balance 증가
- [x] **GREEN**: LedgerService.deposit() 구현
- [x] **REFACTOR**: 정리

- [x] **RED**: 잘못된 계좌 입금 실패 테스트
- [x] **GREEN**: 계좌 검증 로직 추가
- [x] **REFACTOR**: 예외 처리 정리

- [x] **RED**: 중복된 businessRefId 입금 요청 테스트 (Idempotency)
  - Given: 이미 처리된 businessRefId로 재요청
  - Then: 새로운 Transaction 생성 없이 성공 응답 (또는 IdempotencyException)
- [x] **GREEN**: businessRefId 중복 체크 로직 추가
- [x] **REFACTOR**: 멱등성 처리 로직 분리

- [x] PR 생성 및 머지

---

## PR 4: 출금(Withdraw) UseCase 구현 (~200 LOC)

> **POLICY.md 반영**: UseCase별 구현체 분리 원칙에 따라 `WithdrawService`를 별도 생성.
> Domain Entity Factory Methods (`Transaction.createWithdraw`, `JournalEntry.createDebit`, `Balance.withdraw`) 활용.

### TDD Cycle
- [x] **RED**: 출금 성공 테스트 작성
  - Given: 계좌, 출금액, 충분한 잔액
  - When: withdraw 호출
  - Then: Transaction(POSTED) 생성, JournalEntry(DEBIT) 생성, Balance 감소
- [x] **GREEN**: `WithdrawService.withdraw()` 구현
- [x] **REFACTOR**: 정리

- [x] **RED**: 잔액 부족 시 출금 실패 테스트
  - Given: 계좌, 출금액 > 가용잔액
  - When: withdraw 호출
  - Then: InsufficientBalanceException 발생
- [x] **GREEN**: 잔액 검증 로직 추가
- [x] **REFACTOR**: 예외 클래스 정리

- [x] **RED**: 중복된 businessRefId 출금 요청 테스트 (Idempotency)
- [x] **GREEN**: businessRefId 중복 체크 로직 추가

- [X] PR 생성 및 머지


---

## PR 5: 역분개(Reversal) UseCase 구현 (~250 LOC)

> **POLICY.md 반영**: UseCase별 구현체 분리 원칙에 따라 `ReversalService`를 별도 생성.
> **N+1 쿼리 방지**: 배치 조회/업데이트 적용 (`findByAccountIds`, `saveAll`, `updateAll`).
> **Rich Domain Model**: 검증 로직을 `Transaction` 엔티티로 이동.

### TDD Cycle
- [x] **RED**: 역분개 성공 테스트 작성
  - Given: POSTED 상태의 Transaction
  - When: reverse 호출
  - Then: 원 Transaction → REVERSED, 새 Transaction(reversalOf) 생성, Balance 복구
- [x] **GREEN**: `ReversalService.reverse()` 구현
- [x] **REFACTOR**: 배치 연산으로 리팩토링 (N+1 방지)

- [x] **RED**: 이미 REVERSED된 트랜잭션 역분개 실패 테스트
- [x] **GREEN**: 상태 검증 로직 추가 (`Transaction.validateCanBeReversed()`)
- [x] **REFACTOR**: 검증 로직을 Domain Entity로 이동

- [x] **RED**: PENDING 상태 트랜잭션 역분개 실패 테스트
- [x] **GREEN**: POSTED만 역분개 가능하도록 검증
- [x] **REFACTOR**: `InvalidTransactionStateException` 도메인 예외 추가

- [x] **RED**: 빈 분개 목록 검증 테스트 (데이터 정합성)
- [x] **GREEN**: `validateJournalEntriesExist()` 구현

- [x] README.md 업데이트 (역분개 패턴 설명 추가)
- [x] PR 생성 및 머지

---

## PR 6: 동시성 테스트 및 Deep Dive 문서 (~200 LOC)

### 코드 작성 전
- POLICY.md 참조 필수

### 동시성 테스트
- [x] **RED**: 동일 계좌 동시 출금 테스트 (Race Condition)
  - Given: 잔액 1000원, 2개 스레드가 각각 1000원 출금 시도
  - Then: 하나만 성공, 하나는 InsufficientBalanceException
- [x] **GREEN**: 동시성 제어 적용 (Optimistic Lock 또는 Pessimistic Lock)
- [x] **REFACTOR**: 정리

### Deep Dive 문서 작성
- [x] `docs/deep-dive/spring-transactional.md` 작성
  - [x] @Transactional 프록시 동작 원리
  - [x] Self-invocation 문제
  - [x] Propagation 옵션
  - [x] Isolation Level 선택
  - [x] ReadOnly 최적화

- [x] PR 생성 및 머지

---

## Hexagonal Architecture 패키지 구조

> **POLICY.md 반영**: 
> - UseCase별 구현체 (`DepositService`, `WithdrawService`, `ReversalService`) 분리.
> - 통합형 Port 원칙에 따라 Aggregate별 단일 Port 사용.
> - 공통 로직(유효성 검증 등)이 필요한 경우 `domain/service/` 또는 `application/component/`로 추출.

```
kuku-core-ledger/src/main/java/com/securities/kuku/ledger/
├── domain/           # 순수 도메인 (POJO)
│   ├── Account, Transaction, JournalEntry, Balance
│   ├── AccountType, TransactionType, TransactionStatus
│   ├── InvalidTransactionStateException, InsufficientBalanceException
│   └── service/      # (Optional) 공통 도메인 로직
│       └── AccountValidator, BalanceCalculator (필요시)
├── application/      # Use Case (비즈니스 로직)
│   ├── port/
│   │   ├── in/       # Inbound Port
│   │   │   ├── DepositUseCase, WithdrawUseCase, ReversalUseCase
│   │   │   └── command/
│   │   │       ├── DepositCommand, WithdrawCommand, ReversalCommand
│   │   └── out/      # Outbound Port (통합형)
│   │       ├── TransactionPort   # findById, findByBusinessRefId, save, update
│   │       ├── AccountPort       # findById
│   │       ├── BalancePort       # findByAccountId, findByAccountIds, update, updateAll
│   │       └── JournalEntryPort  # save, saveAll, findByTransactionId
│   └── service/      # UseCase 구현체 (1 Interface, 1 Implementation)
│       ├── DepositService   (implements DepositUseCase)
│       ├── WithdrawService  (implements WithdrawUseCase)
│       └── ReversalService  (implements ReversalUseCase)
└── adapter/          # 외부 세계 (Week 4 이후)
    ├── in/web/       # REST Controller
    └── out/persistence/  # JPA Repository
```

---

## 테스트 파일 구조

```
kuku-core-ledger/src/test/java/com/securities/kuku/ledger/
├── domain/           # 도메인 단위 테스트 (현재)
│   ├── AccountTest, TransactionTest, JournalEntryTest, BalanceTest
│   └── TransactionStatusTest
└── application/
    └── service/
        ├── DepositServiceTest
        ├── WithdrawServiceTest
        └── ReversalServiceTest
```

---

## Week 4 Preview: UNKNOWN 상태 구현 계획

> Week 3에서 논의된 PENDING/UNKNOWN 상태를 Week 4에서 구현하기로 결정.

### 결정 배경

| 비교 항목 | Week 4 | Week 12 |
|-----------|--------|---------|
| 리팩토링 비용 | 낮음 (단일 서비스) | 높음 (MSA 전체 수정) |
| 설계 일관성 | 높음 (처음부터 포함) | 낮음 (나중에 추가) |
| 면접 스토리 | "초기부터 Unknown 고려" | "나중에 추가" (임팩트↓) |
| 연관 작업 | 장애 시나리오 테스트와 연결 | 작업량 폭주 위험 |

**결론**: Week 4에서 UNKNOWN 상태를 구현하고, Week 12에서는 자동 복구 배치만 구현.

### Week 4에서 구현할 항목

- [ ] `TransactionStatus.UNKNOWN` 추가
- [ ] PENDING → POSTED 2단계 전환 로직
- [ ] Unknown 상태 감지 기준 정의 (Timeout, Exception 등)
- [ ] Unknown 상태 Transaction 조회 API
- [ ] 장애 시나리오 테스트에서 UNKNOWN 상태 검증
  - DB Lock 경쟁 시나리오
  - Kafka 다운 시 Outbox 패턴 검증

### Week 12에서 구현할 항목

- [ ] Unknown 상태 자동 복구/재처리 배치 로직
- [ ] 장애 복구 플로우 다이어그램 작성
