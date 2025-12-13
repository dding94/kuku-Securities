# 입출금 트랜잭션 Sequence Diagram

> 이 문서는 입금(Deposit)과 출금(Withdraw) 트랜잭션의 흐름을 설명합니다.

---

## 입금 (Deposit) 플로우

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant S as DepositService
    participant TP as TransactionPort
    participant AP as AccountPort
    participant BP as BalancePort
    participant JP as JournalEntryPort
    participant DB as Database

    C->>S: deposit(DepositCommand)
    
    rect rgb(230, 245, 255)
    Note over S,DB: @Transactional 경계
    
    Note over S: 1. 멱등성 체크
    S->>TP: findByBusinessRefId(businessRefId)
    TP->>DB: SELECT * FROM transactions WHERE business_ref_id = ?
    DB-->>TP: null (신규 요청)
    TP-->>S: Optional.empty()

    Note over S: 2. 계좌 검증
    S->>AP: findById(accountId)
    AP->>DB: SELECT * FROM accounts WHERE id = ?
    DB-->>AP: Account
    AP-->>S: Account

    Note over S: 3. 잔고 조회
    S->>BP: findByAccountId(accountId)
    BP->>DB: SELECT * FROM balances WHERE account_id = ?
    DB-->>BP: Balance
    BP-->>S: Balance

    Note over S: 4. 도메인 로직 실행
    S->>S: Transaction.createDeposit()
    S->>S: JournalEntry.createCredit()
    S->>S: Balance.deposit(amount)

    Note over S: 5. 영속화
    S->>TP: save(Transaction)
    TP->>DB: INSERT INTO transactions
    DB-->>TP: Transaction (with ID)
    TP-->>S: Transaction

    S->>JP: save(JournalEntry)
    JP->>DB: INSERT INTO journal_entries
    DB-->>JP: JournalEntry
    JP-->>S: JournalEntry

    S->>BP: update(Balance)
    BP->>DB: UPDATE balances SET amount = ?, version = version + 1
    DB-->>BP: 1 row updated
    BP-->>S: Balance
    
    end

    S-->>C: void (성공)
```

---

## 출금 (Withdraw) 플로우

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant S as WithdrawService
    participant TP as TransactionPort
    participant AP as AccountPort
    participant BP as BalancePort
    participant JP as JournalEntryPort
    participant DB as Database

    C->>S: withdraw(WithdrawCommand)
    
    rect rgb(230, 245, 255)
    Note over S,DB: @Transactional 경계
    
    Note over S: 1. 멱등성 체크
    S->>TP: findByBusinessRefId(businessRefId)
    TP->>DB: SELECT * FROM transactions WHERE business_ref_id = ?
    DB-->>TP: null
    TP-->>S: Optional.empty()

    Note over S: 2. 계좌 검증
    S->>AP: findById(accountId)
    AP->>DB: SELECT * FROM accounts
    DB-->>AP: Account
    AP-->>S: Account

    Note over S: 3. 잔고 조회 & 검증
    S->>BP: findByAccountId(accountId)
    BP->>DB: SELECT * FROM balances
    DB-->>BP: Balance
    BP-->>S: Balance

    alt 잔액 부족
        S->>S: Balance.withdraw(amount)
        S--xC: InsufficientBalanceException
    end

    Note over S: 4. 도메인 로직 실행
    S->>S: Transaction.createWithdraw()
    S->>S: JournalEntry.createDebit()
    S->>S: Balance.withdraw(amount)

    Note over S: 5. 영속화
    S->>TP: save(Transaction)
    TP->>DB: INSERT INTO transactions
    
    S->>JP: save(JournalEntry)
    JP->>DB: INSERT INTO journal_entries
    
    S->>BP: update(Balance)
    BP->>DB: UPDATE balances (Optimistic Lock)
    
    Note over BP,DB: version 불일치 시<br/>OptimisticLockException
    
    end

    S-->>C: void (성공)
```

---

## 주요 설계 원칙

### 1. 멱등성 (Idempotency)
- `businessRefId`로 중복 요청 감지
- 이미 처리된 요청은 재처리하지 않음

### 2. 낙관적 락 (Optimistic Lock)
- `Balance.version` 필드 활용
- 동시 수정 시 `ObjectOptimisticLockingFailureException` 발생

### 3. 이중 부기 (Double-Entry)
- 모든 거래는 `JournalEntry`로 기록
- `DEBIT`/`CREDIT`를 통한 추적 가능
