# C4 Component Diagram: Ledger Service 내부 구조

> Kuku Securities - Core Ledger 서비스의 컴포넌트 레벨 아키텍처

---

## 개요

이 다이어그램은 Ledger 서비스의 **Hexagonal Architecture (Ports & Adapters)** 기반 내부 구조를 보여줍니다.

---

## C4 Component Diagram

```mermaid
flowchart TB
    subgraph External["🌐 External"]
        Client["👤 Client/API Consumer"]
        MySQL[("🗄️ MySQL")]
        Kafka["📨 Kafka (Week 7)"]
    end

    subgraph Ledger["🏦 Core Ledger Service"]
        
        subgraph Adapter["Adapter Layer"]
            direction TB
            Controller["🌐 LedgerController\n(향후 구현)"]
            
            subgraph Persistence["Persistence Adapters"]
                TxAdapter["TransactionPersistenceAdapter"]
                BalanceAdapter["BalancePersistenceAdapter"]
                JournalAdapter["JournalEntryPersistenceAdapter"]
                OutboxAdapter["OutboxEventPersistenceAdapter"]
            end
        end
        
        subgraph Application["Application Layer"]
            direction TB
            
            subgraph UseCases["Inbound Ports"]
                DepositUC["DepositUseCase"]
                WithdrawUC["WithdrawUseCase"]
                ReversalUC["ReversalUseCase"]
                ConfirmUC["ConfirmTransactionUseCase"]
            end
            
            subgraph Services["Services"]
                DepositSvc["DepositService\n@Retryable"]
                WithdrawSvc["WithdrawService\n@Retryable"]
                ReversalSvc["ReversalService"]
                ConfirmSvc["ConfirmTransactionService"]
                OutboxRecorder["OutboxEventRecorder"]
            end
            
            subgraph OutPorts["Outbound Ports"]
                TxPort["TransactionPort"]
                BalancePort["BalancePort"]
                JournalPort["JournalEntryPort"]
                OutboxPort["OutboxEventPort"]
            end
        end
        
        subgraph Domain["Domain Layer"]
            direction TB
            Transaction["Transaction\n(상태 전이)"]
            Balance["Balance\n(@Version)"]
            JournalEntry["JournalEntry\n(이중부기)"]
            OutboxEvent["OutboxEvent"]
            
            subgraph Events["Domain Events"]
                LedgerPosted["LedgerPostedEvent"]
                LedgerReversed["LedgerReversedEvent"]
            end
        end
    end

    %% Relationships
    Client --> Controller
    Controller --> UseCases
    
    DepositUC -.-> DepositSvc
    WithdrawUC -.-> WithdrawSvc
    ReversalUC -.-> ReversalSvc
    ConfirmUC -.-> ConfirmSvc
    
    Services --> Domain
    Services --> OutPorts
    Services --> OutboxRecorder
    OutboxRecorder --> Events
    
    TxPort -.-> TxAdapter
    BalancePort -.-> BalanceAdapter
    JournalPort -.-> JournalAdapter
    OutboxPort -.-> OutboxAdapter
    
    Persistence --> MySQL
    OutboxAdapter -.-> Kafka

    %% Styling
    classDef domain fill:#e1f5fe,stroke:#01579b
    classDef application fill:#fff3e0,stroke:#e65100
    classDef adapter fill:#f3e5f5,stroke:#4a148c
    classDef external fill:#fce4ec,stroke:#880e4f
    
    class Transaction,Balance,JournalEntry,OutboxEvent,LedgerPosted,LedgerReversed domain
    class DepositSvc,WithdrawSvc,ReversalSvc,ConfirmSvc,OutboxRecorder,DepositUC,WithdrawUC,ReversalUC,ConfirmUC application
    class TxAdapter,BalanceAdapter,JournalAdapter,OutboxAdapter,Controller adapter
    class Client,MySQL,Kafka external
```

---

## 컴포넌트 상세

### Domain Layer

| 컴포넌트 | 책임 | 핵심 로직 |
|:---------|:-----|:---------|
| **Transaction** | 트랜잭션 생명주기 관리 | 상태 전이 (PENDING→POSTED→REVERSED) |
| **JournalEntry** | 이중부기 분개 기록 | DEBIT/CREDIT 엔트리, 역분개 |
| **Balance** | 계좌 잔액 관리 | Optimistic Lock (@Version) |
| **Account** | 계좌 정보 | USER_CASH, SYSTEM_FEE 등 |
| **OutboxEvent** | Outbox 이벤트 | PENDING→PROCESSED 상태 관리 |
| **LedgerEvent** | 도메인 이벤트 | LedgerPostedEvent, LedgerReversedEvent |

### Application Layer

| 컴포넌트 | 책임 | 특이사항 |
|:---------|:-----|:---------|
| **DepositService** | 입금 처리 | @Retryable 적용 |
| **WithdrawService** | 출금 처리 | 잔액 검증 포함 |
| **ReversalService** | 역분개 처리 | 원거래 상태 검증 |
| **ConfirmTransactionService** | PENDING 확정 | Week 7 외부 체결 연동용 |
| **OutboxEventRecorder** | Outbox 기록 | 도메인 이벤트 생성 |

### Adapter Layer

| 컴포넌트 | 책임 | 구현 |
|:---------|:-----|:-----|
| **TransactionPersistenceAdapter** | 트랜잭션 영속성 | JPA + Spring Data |
| **BalancePersistenceAdapter** | 잔액 영속성 | Optimistic Lock 처리 |
| **JournalEntryPersistenceAdapter** | 분개 영속성 | 배치 저장 지원 |
| **OutboxEventPersistenceAdapter** | Outbox 영속성 | 폴링 조회 지원 |

---

## 데이터 흐름

### 입금 플로우

```mermaid
sequenceDiagram
    participant C as Controller
    participant S as DepositService
    participant T as Transaction
    participant J as JournalEntry
    participant B as Balance
    participant O as OutboxRecorder
    participant DB as MySQL

    C->>S: deposit(command)
    S->>T: create(POSTED)
    S->>J: createEntries(DEBIT/CREDIT)
    S->>B: deposit(amount)
    S->>O: record(LedgerPostedEvent)
    S->>DB: saveAll()
    S-->>C: Transaction
```

---

## 기술 결정 사항

| 결정 | 선택 | 이유 |
|:-----|:-----|:-----|
| 동시성 제어 | Optimistic Lock | 읽기 비율 높음, 충돌 시 재시도 |
| 이벤트 발행 | Outbox 패턴 | 트랜잭션 보장, At-Least-Once |
| 상태 전이 | Domain Entity 캡슐화 | 불변식 보장 |
