# Ledger 이벤트 흐름도

## Outbox 패턴 기반 이벤트 발행 흐름

```mermaid
sequenceDiagram
    participant Client
    participant Service as DepositService
    participant Domain as Transaction/Balance
    participant Outbox as OutboxEventRecorder
    participant DB as Database

    Client->>Service: deposit(command)
    
    rect rgb(200, 230, 200)
        Note over Service, DB: Single Transaction
        Service->>Domain: Create Transaction
        Service->>DB: Save Transaction
        Service->>Domain: Create JournalEntry
        Service->>DB: Save JournalEntry
        Service->>Domain: Update Balance
        Service->>DB: Save Balance
        Service->>Outbox: record(LedgerPostedEvent)
        Outbox->>DB: Save OutboxEvent (PENDING)
    end
    
    Service-->>Client: Success
```

## Kafka 발행 흐름 (PR 8 예정)

```mermaid
sequenceDiagram
    participant Scheduler as @Scheduled
    participant Publisher as OutboxEventPublisher
    participant DB as Database
    participant Kafka as Kafka

    loop Every 2 seconds
        Scheduler->>Publisher: poll()
        Publisher->>DB: findPendingEvents(100)
        DB-->>Publisher: List<OutboxEvent>
        
        loop For each event
            Publisher->>Kafka: send(topic, event)
            alt Success
                Kafka-->>Publisher: Ack
                Publisher->>DB: markAsProcessed(eventId)
            else Failure
                Kafka-->>Publisher: Error
                Publisher->>DB: incrementRetryCount(eventId)
                Note over Publisher: Max 5 retries → FAILED
            end
        end
    end
```

## 이벤트 타입

| Event Type | Trigger | Payload 주요 필드 |
|------------|---------|-------------------|
| `LEDGER_POSTED` | 입금/출금 완료 | transactionId, accountId, amount, transactionType |
| `LEDGER_REVERSED` | 역분개 완료 | reversalTransactionId, originalTransactionId, reason |

## Outbox Status 상태 전이

```mermaid
stateDiagram-v2
    [*] --> PENDING: 이벤트 저장
    PENDING --> PROCESSED: Kafka 발행 성공
    PENDING --> PENDING: Kafka 발행 실패 (retry < 5)
    PENDING --> FAILED: Kafka 발행 실패 (retry >= 5)
    FAILED --> [*]: 수동 조치 필요
```

## 장점

1. **원자성 보장**: 비즈니스 로직과 이벤트 저장이 동일 트랜잭션
2. **신뢰성**: Kafka 다운 시에도 이벤트 유실 없음
3. **이벤트 재발행**: PENDING 상태로 남아있어 복구 가능
4. **감사 로그**: Outbox 테이블이 이벤트 히스토리 역할
