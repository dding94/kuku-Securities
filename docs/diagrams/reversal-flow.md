# 역분개 (Reversal) 플로우 다이어그램

> 금융 시스템에서 잘못된 거래를 취소하는 **역분개(Reversal)** 패턴을 설명합니다.

---

## 역분개란?

금융 원장에서는 한 번 기록된 거래를 **절대 삭제하거나 수정하지 않습니다**.  
대신, **반대 방향의 거래를 새로 만들어서 상쇄**합니다.

```mermaid
graph LR
    subgraph "원본 거래"
        T1["Tx #1: 입금 1000원<br/>Status: POSTED → REVERSED"]
    end
    
    subgraph "역분개 거래"
        T2["Tx #2: 역분개 1000원<br/>Status: POSTED<br/>reversalOfTransactionId: 1"]
    end
    
    T1 -->|"역분개 연결"| T2
    
    style T1 fill:#ffcccc,stroke:#d84315
    style T2 fill:#c8e6c9,stroke:#388e3c
```

---

## 역분개 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RS as ReversalService
    participant TP as TransactionPort
    participant JP as JournalEntryPort
    participant BP as BalancePort
    participant DB as Database

    C->>RS: reverse(ReversalCommand)
    
    rect rgb(255, 245, 238)
    Note over RS,DB: @Transactional 경계
    
    Note over RS: 1. 원본 트랜잭션 조회
    RS->>TP: findById(originalTransactionId)
    TP->>DB: SELECT * FROM transactions WHERE id = ?
    DB-->>TP: Transaction (POSTED)
    TP-->>RS: Transaction

    Note over RS: 2. 상태 검증 (Domain Logic)
    RS->>RS: transaction.validateCanBeReversed()
    
    alt 이미 REVERSED 상태
        RS--xC: InvalidTransactionStateException
    else PENDING 상태
        RS--xC: InvalidTransactionStateException
    end

    Note over RS: 3. 원본 분개 조회
    RS->>JP: findByTransactionId(transactionId)
    JP->>DB: SELECT * FROM journal_entries
    DB-->>JP: List<JournalEntry>
    JP-->>RS: List<JournalEntry>

    Note over RS: 4. 배치 잔고 조회 (N+1 방지)
    RS->>BP: findByAccountIds(accountIds)
    BP->>DB: SELECT * FROM balances WHERE account_id IN (?)
    DB-->>BP: Map<Long, Balance>
    BP-->>RS: Map<Long, Balance>

    Note over RS: 5. 원본 트랜잭션 상태 변경
    RS->>RS: transaction.toReversed()
    RS->>TP: update(reversedTransaction)
    TP->>DB: UPDATE transactions SET status = 'REVERSED'
    DB-->>TP: OK
    TP-->>RS: OK

    Note over RS: 6. 역분개 트랜잭션 생성
    RS->>RS: Transaction.createReversal()
    RS->>TP: save(reversalTransaction)
    TP->>DB: INSERT INTO transactions
    DB-->>TP: Transaction (with ID)
    TP-->>RS: Transaction

    Note over RS: 7. 반대 분개 생성 (배치)
    RS->>RS: createOppositeEntries()
    RS->>JP: saveAll(oppositeEntries)
    JP->>DB: INSERT INTO journal_entries (batch)
    DB-->>JP: OK
    JP-->>RS: OK

    Note over RS: 8. 잔고 복구 (배치)
    RS->>RS: restoreBalances()
    RS->>BP: updateAll(restoredBalances)
    BP->>DB: UPDATE balances (batch)
    DB-->>BP: OK
    BP-->>RS: OK
    
    end

    RS-->>C: void (성공)
```

---

## 상태 전이 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PENDING: 트랜잭션 생성
    PENDING --> POSTED: 확정
    POSTED --> REVERSED: 역분개
    REVERSED --> [*]
    
    note right of PENDING
        아직 확정되지 않은 상태
        (Week 4에서 구현 예정)
    end note
    
    note right of POSTED
        역분개 가능한 유일한 상태
    end note
    
    note right of REVERSED
        더 이상 역분개 불가
        (역분개의 역분개는 별도 트랜잭션)
    end note
```

---

## 분개 상쇄 (JournalEntry Reversal)

```mermaid
flowchart TB
    subgraph "원본 입금 (Tx #1)"
        J1["JournalEntry<br/>accountId: 1<br/>type: CREDIT<br/>amount: 1000"]
    end
    
    subgraph "역분개 (Tx #2)"
        J2["JournalEntry<br/>accountId: 1<br/>type: DEBIT<br/>amount: 1000"]
    end
    
    J1 -->|"반대 타입 생성"| J2
    
    subgraph "결과"
        R["Balance 변화: +1000 - 1000 = 0"]
    end
    
    J1 --> R
    J2 --> R
    
    style J1 fill:#c8e6c9,stroke:#388e3c
    style J2 fill:#ffcccc,stroke:#d84315
    style R fill:#fff9c4,stroke:#fbc02d
```

---

## 핵심 설계 원칙

| 원칙 | 설명 |
|------|------|
| **불변성** | 원본 트랜잭션의 **금액, 타입, businessRefId 등 핵심 데이터**는 절대 수정하지 않음. **상태(status)만** `POSTED` → `REVERSED`로 전이. |
| **추적성** | `reversalOfTransactionId`로 원본-역분개 연결 |
| **도메인 로직** | 검증 로직은 `Transaction.validateCanBeReversed()`에 위치 |
| **배치 처리** | N+1 쿼리 방지를 위해 `findByAccountIds`, `saveAll`, `updateAll` 사용 |
