# [ADR-010] 주문 상태 머신 설계 패턴

*   **Status**: Accepted
*   **Date**: 2026-01-05
*   **Author**: dding94

## 1. Context (배경)

### 1.1. 문제 상황

주문 시스템에서 주문의 생명주기를 관리해야 합니다. 주문은 다음과 같은 상태를 가집니다:

- CREATED: 주문 생성
- VALIDATED: 검증 완료
- FILLED: 체결 완료
- REJECTED: 거부됨
- CANCELLED: 취소됨

각 상태 간의 전이는 명확한 규칙을 따라야 하며, 잘못된 상태 전이는 비즈니스 오류로 처리되어야 합니다.

### 1.2. 설계 목표

1. **타입 안전성**: 컴파일 타임에 상태 전이 규칙 검증
2. **불변성**: 상태 전이 시 새 객체 반환
3. **캡슐화**: 상태 전이 로직을 도메인에 응집
4. **확장성**: 새 상태 추가 시 영향 범위 최소화

## 2. Decision (결정)

**Enum + Domain Method** 방식을 선택합니다.

```java
public enum OrderStatus {
    CREATED, VALIDATED, FILLED, REJECTED, CANCELLED;
    
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == VALIDATED || target == REJECTED;
            case VALIDATED -> target == FILLED || target == REJECTED || target == CANCELLED;
            case FILLED, REJECTED, CANCELLED -> false;
        };
    }
}

public class Order {
    public Order validate(Instant now) {
        if (this.status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(...);
        }
        return withStatusAndTime(OrderStatus.VALIDATED, now);
    }
}
```

## 3. Alternatives Considered (대안 분석)

### 3.1. Enum + Domain Method ✅ (선택)

| 장점 | 단점 |
|------|------|
| 단순한 구조 | 상태 추가 시 switch 문 수정 필요 |
| 적은 코드량 | 복잡한 상태 로직 시 메서드 비대화 |
| switch expression으로 컴파일 타임 검증 | |
| 기존 Transaction.java 패턴과 일관성 | |

### 3.2. State Pattern (GoF) ❌

```java
interface OrderState {
    Order validate(Order order, Instant now);
    Order reject(Order order, RejectionReason reason, Instant now);
    Order fill(Order order, BigDecimal price, BigDecimal qty, Instant now);
    Order cancel(Order order, Instant now);
}

class CreatedState implements OrderState { ... }
class ValidatedState implements OrderState { ... }
```

| 장점 | 단점 |
|------|------|
| OCP 준수 (새 상태 추가 용이) | 클래스 수 증가 (상태 수 × 2) |
| 상태별 로직 분리 | 보일러플레이트 코드 |
| 복잡한 상태 로직에 적합 | 현재 요구사항 대비 과잉 설계 |

## 4. Rationale (선택 근거)

### 4.1. YAGNI 원칙

현재 상태는 5개(CREATED, VALIDATED, FILLED, REJECTED, CANCELLED)이며, 상태별 로직이 단순합니다.
Week 7에서 PARTIALLY_FILLED 추가 예정이지만, switch expression 사용으로 누락 케이스가 컴파일 에러로 감지됩니다.

### 4.2. 기존 패턴과 일관성

`kuku-core-ledger`의 `Transaction.java`가 동일한 패턴을 사용합니다:

```java
// Transaction.java
public Transaction toReversed() {
    validateCanBeReversed();
    return withStatus(TransactionStatus.REVERSED);
}

// Order.java (동일 패턴)
public Order validate(Instant now) {
    if (this.status != OrderStatus.CREATED) { ... }
    return withStatusAndTime(OrderStatus.VALIDATED, now);
}
```

### 4.3. 리팩토링 용이성

상태가 10개 이상으로 증가하거나, 상태별 복잡한 비즈니스 로직이 추가되면 State Pattern으로 리팩토링합니다.
현재 구조에서 리팩토링 비용은 낮습니다 (메서드 단위로 분리하면 됨).

## 5. Consequences (결과)

### 5.1. 기대 효과

| 측면 | 효과 |
|------|------|
| **유지보수성** | 상태 전이 로직이 Order 엔티티에 응집 |
| **테스트 용이성** | 단위 테스트로 모든 전이 규칙 검증 가능 |
| **확장 안전성** | switch expression으로 누락 케이스 컴파일 에러 |

### 5.2. 주의 사항

1. **switch 문 동기화**: `OrderStatus.canTransitionTo()`와 `Order`의 전이 메서드 간 일관성 유지
2. **상태 추가 시**: Enum 값 추가 → switch 문 업데이트 → 테스트 추가

### 5.3. Week 7 확장 계획

PARTIALLY_FILLED 상태 추가 시:

```java
public enum OrderStatus {
    CREATED, VALIDATED, PARTIALLY_FILLED, FILLED, REJECTED, CANCELLED;
    
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == VALIDATED || target == REJECTED;
            case VALIDATED -> target == PARTIALLY_FILLED || target == FILLED 
                           || target == REJECTED || target == CANCELLED;
            case PARTIALLY_FILLED -> target == FILLED || target == CANCELLED;
            case FILLED, REJECTED, CANCELLED -> false;
        };
    }
}
```

## 6. References

*   [Transaction.java](file:///Users/mg/Desktop/MG/01-이직/01-개인프로젝트/kuku/kuku-core-ledger/src/main/java/com/securities/kuku/ledger/domain/Transaction.java) - 참고한 기존 패턴
*   [State Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/state)
*   Martin Fowler - "Patterns of Enterprise Application Architecture"
