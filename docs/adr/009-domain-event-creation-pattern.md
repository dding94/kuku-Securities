# [ADR-009] Domain Event 생성 패턴 선택: 명시적 호출 vs Event Collection

*   **Status**: Accepted
*   **Date**: 2025-12-27
*   **Author**: dding94
*   **Context**: Outbox Pattern 구현 (Week 4)

---

## 1. Context (배경)

Domain Event를 Outbox에 저장하는 방식을 구현하면서, **어느 레이어에서 어떻게 이벤트를 생성할 것인가**에 대한 설계 결정이 필요했다.

### 1.1. 문제 상황

Outbox Pattern을 적용하면서, 도메인 엔티티의 상태 변화를 이벤트로 표현해야 한다.

**Transaction (Ledger Aggregate)의 경우:**
- 입금 → `LedgerPostedEvent` (1개)
- 출금 → `LedgerPostedEvent` (1개)
- 역분개 → `LedgerReversedEvent` (1개)
- **특징: 1개 상태 변경 = 1개 이벤트 (단순)**

**Order (향후 구현 예정)의 경우:**
- 주문 생성 → `OrderCreatedEvent`, `InventoryReservedEvent`, `LedgerHoldRequestedEvent` (3개)
- **특징: 1개 상태 변경 = N개 이벤트 (복잡)**

### 1.2. Why-Driven Question

**핵심 질문:**
> "도메인 이벤트 생성 책임을 어디에 둘 것인가?"

**배경:**
- DDD 원칙에 따르면 Aggregate Root가 자신의 Domain Event를 생성해야 함
- 하지만 모든 Aggregate가 동일한 복잡도를 가지는 것은 아님
- 과도한 추상화는 오히려 가독성을 해칠 수 있음

---

## 2. Decision (결정)

**Aggregate 특성에 따라 두 가지 패턴을 선택적으로 적용한다.**

| Aggregate | 패턴 | 이유 |
|-----------|------|------|
| **Transaction (Ledger)** | **명시적 호출** | 1:1 매핑, 명시적 가독성 우선 |
| **Order (향후)** | **Event Collection** | 1:N 매핑, 누락 방지 필요 |

---

## 3. Alternatives Considered (대안 분석)

### 3.1. Option A: 명시적 호출 (Explicit Invocation) ✅ (Transaction에 적용)

**구현:**
```java
// Transaction.java (도메인 엔티티)
public LedgerPostedEvent toPostedEvent(
    Long accountId, 
    BigDecimal amount, 
    TransactionType type) {
    return LedgerPostedEvent.of(
        this.id,          // 내부 상태
        accountId,
        amount,
        type,
        this.createdAt    // 내부 상태
    );
}

// DepositService.java (Application Layer)
outboxEventRecorder.record(
    savedTransaction.toPostedEvent(
        command.accountId(),
        command.amount(),
        TransactionType.DEPOSIT
    )
);
```

| 장점 | 단점 |
|------|------|
| 명시적: 서비스 코드를 읽으면 "어떤 이벤트가 발행되는지" 즉시 파악 | N개 이벤트 발행 시 누락 가능성 |
| 불변성 유지: Transaction 불변 객체 유지 가능 | 서비스가 "어떤 이벤트를 발행해야 하는지" 알아야 함 |
| 단순성: 추가 인프라(List 관리) 불필요 | 복잡한 Aggregate에서는 비효율 |
| Tell, Don't Ask 준수: 서비스가 ID를 꺼내지 않고 엔티티에게 위임 | - |

---

### 3.2. Option B: Event Collection (향후 Order에 적용 예정)

**구현:**
```java
// AbstractAggregateRoot.java (Base Entity)
public abstract class AbstractAggregateRoot<T> {
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}

// Order.java (Aggregate Root)
public class Order extends AbstractAggregateRoot<Order> {
    public static Order place(PlaceOrderCommand cmd) {
        Order order = new Order(...);
        
        // 도메인 엔티티가 자신의 이벤트를 모두 등록
        order.registerEvent(new OrderCreatedEvent(...));
        order.registerEvent(new InventoryReservedEvent(...));
        order.registerEvent(new LedgerHoldRequestedEvent(...));
        
        return order;
    }
}

// OrderService.java (Application Layer)
Order order = Order.place(command);
orderPort.save(order);

// 서비스는 이벤트가 무엇인지 몰라도 됨
order.getDomainEvents().forEach(outboxEventRecorder::record);
order.clearDomainEvents();
```

| 장점 | 단점 |
|------|------|
| 완전한 캡슐화: 서비스가 "어떤 이벤트를 발행해야 하는지" 몰라도 됨 | 불필요한 복잡성: 단순한 Aggregate에는 과도한 엔지니어링 |
| 누락 방지: 개발자가 이벤트 발행을 깜빡할 위험 제거 | 가변 상태 도입: `List<DomainEvent>` 관리로 불변성 위반 |
| N개 이벤트 대응: 복잡한 비즈니스 로직에 유리 | 명시성 감소: 서비스 코드만 봐서는 어떤 이벤트가 발행되는지 알 수 없음 |
| 자동화 가능: Spring AOP로 `@Transactional` 종료 시점 자동 발행 가능 | 테스트 복잡도: `clearDomainEvents()` 호출 누락 시 중복 발행 위험 |

---

### 3.3. Option C: 모든 Aggregate에 Event Collection 강제 적용 ❌

전체 시스템에서 단일 패턴만 사용하여 일관성 확보.

| 장점 | 단점 |
|------|------|
| 일관성: 모든 Aggregate가 동일한 방식 | 과도한 추상화: Transaction처럼 단순한 경우에도 복잡성 증가 |
| 학습 곡선: 새 개발자가 하나의 패턴만 학습 | 불변성 희생: 모든 Aggregate가 가변 상태 도입 |

> **거부 이유:** "Silver Bullet은 없다." 모든 문제에 하나의 해법을 강제하는 것은 실용적이지 않음.

---

## 4. Implementation Details (구현 상세)

### 4.1. 현재 구현 (Week 4: Transaction)

**패턴: 명시적 호출 (Option A)**

```java
// 1. Transaction에 메서드 추가
public LedgerPostedEvent toPostedEvent(...) { ... }
public LedgerReversedEvent toReversedEvent(...) { ... }

// 2. 서비스에서 명시적 호출
outboxEventRecorder.record(
    savedTransaction.toPostedEvent(...)
);
```

**적용 이유:**
- Transaction은 1개 상태 변경 = 1개 이벤트
- 명시적 호출이 더 읽기 쉬움
- 불변 객체 유지

---

### 4.2. 향후 구현 (Week 5-7: Order)

**패턴: Event Collection (Option B)**

```java
// 1. AbstractAggregateRoot 베이스 클래스 생성
// 2. Order가 AbstractAggregateRoot 상속
// 3. 서비스에서 getDomainEvents() 일괄 발행
```

**적용 이유:**
- Order는 1개 상태 변경 = N개 이벤트
- 이벤트 누락 방지 필수
- 도메인 캡슐화 강화

---

### 4.3. 패턴 선택 가이드라인

**POLICY.md 업데이트 (Section 7):**

| Aggregate 특성 | 권장 패턴 | 근거 |
|---------------|----------|------|
| **1:1 이벤트 매핑** | 명시적 호출 | 가독성, 불변성 우선 |
| **1:N 이벤트 매핑** | Event Collection | 누락 방지, 캡슐화 우선 |
| **이벤트 수가 동적 변경** | Event Collection | 유연성 필요 |

---

## 5. Consequences (결과)

### 5.1. 기대 효과

| 측면 | 변화 |
|------|------|
| **유연성** | 각 Aggregate 특성에 맞는 최적의 패턴 적용 |
| **가독성** | 단순한 경우는 명시적, 복잡한 경우는 캡슐화 |
| **유지보수성** | Transaction 불변성 유지, Order 확장성 확보 |
| **학습 곡선** | 합리적 수준: 2가지 패턴만 존재 |

### 5.2. 트레이드오프

| 비용 | 설명 |
|------|------|
| **일관성 감소** | 시스템 내 2가지 패턴 공존 |
| **문서화 필요** | "어느 경우에 어떤 패턴을 쓰는지" 명확한 가이드 필요 |
| **리뷰 부담** | PR 리뷰 시 "이 Aggregate는 어떤 패턴이 적합한가?" 판단 필요 |

### 5.3. 실무 정당성

**Google, Netflix 등 대규모 시스템:**
- 단일 패턴 강제보다 **"적재적소(Right Tool for the Job)"** 원칙 선호
- 각 도메인 특성에 맞는 다양한 패턴 공존

**면접 설명 포인트:**
> "Transaction은 1:1 이벤트 매핑이라 명시적 호출이 더 명확했고,  
> Order는 1:N 이벤트 매핑이라 Event Collection이 안전했습니다.  
> 두 패턴 모두 Trade-off를 고려한 **의도적 선택**입니다."

---

## 6. Future Considerations (후속 고려사항)

### 6.1. Week 5-7: Order 구현 시

- `AbstractAggregateRoot` 베이스 클래스 설계
- Event Collection 패턴 적용
- Transaction도 동일 패턴으로 통일할지 재검토

### 6.2. Week 12: 회고 시점

- 두 패턴 운영 경험 정리
- "어떤 경우에 어떤 패턴이 더 효과적이었는가?" 데이터 수집
- 필요시 POLICY.md 가이드라인 업데이트

---

## 7. References

- [Martin Fowler - Domain Event](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Vaughn Vernon - Implementing Domain-Driven Design](https://learning.oreilly.com/library/view/implementing-domain-driven-design/9780133039900/)
- [Spring Data - AbstractAggregateRoot](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/domain/AbstractAggregateRoot.html)
- `/docs/POLICY.md` - Section 7: Domain Event 생성 원칙
- `/docs/adr/008-outbox-pattern.md` - Outbox Pattern 설계
