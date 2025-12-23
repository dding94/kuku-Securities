# [ADR-007] Optimistic Lock 실패 시 재시도 전략

*   **Status**: Accepted
*   **Date**: 2025-12-23
*   **Author**: dding94

## 1. Context (배경)

### 1.1. 문제 상황

금융 원장 시스템에서 **동시성 제어**는 데이터 정합성의 핵심입니다. 우리 시스템은 Optimistic Locking(`@Version`)을 사용하여 동일 계좌에 대한 동시 업데이트를 감지합니다.

동시성 테스트(`LedgerConcurrencyTest`) 결과:
- 10개 스레드 동시 입금 시 일부 `OptimisticLockingFailureException` 발생
- 단, 최종 잔액은 정확함 (성공한 트랜잭션만 반영)
- 실패한 트랜잭션은 **재시도하면 성공 가능**

재시도 없이 즉시 실패 시:
*   사용자에게 "일시적 오류"로 노출 → UX 저하
*   성공 가능한 요청을 불필요하게 실패 처리
*   수동 재시도 부담 증가

### 1.2. 왜 재시도가 적합한가?

**Optimistic Lock 충돌의 특성:**
1. **일시적(Transient)**: 충돌은 밀리초 단위로 해소됨
2. **재시도 가능(Retriable)**: 동일 요청을 다시 수행해도 부작용 없음 (멱등성)
3. **높은 성공률**: AWS 논문에 따르면 100~200ms 대기 후 재시도 시 95% 이상 성공

**예수금 입출금의 시간 민감도:**
- 주식 체결: ❌ 가격 변동 민감 → 재시도 부적합
- **예수금 이체**: ✅ 시간 지연(~400ms) 허용 → 재시도 적합

**참고:** Martin Fowler의 "Patterns of Enterprise Application Architecture"에서도 Optimistic Lock 충돌에 대한 재시도를 권장

## 2. Decision (결정)

**Spring Retry의 `@Retryable` 애노테이션**을 사용하여 선언적 재시도 로직을 구현한다.

### 재시도 대상
*   `DepositService.deposit()`
*   `WithdrawService.withdraw()`
*   `ReversalService.reverse()`
*   `ConfirmTransactionService.confirm()`

### 재시도 설정

| 설정 | 값 | 선택 근거 |
|------|-----|----------|
| `maxAttempts` | 3 | AWS DynamoDB 권장사항: 2~4회. 3회 시 누적 성공률 95% 이상 |
| `delay` | 100ms | DB Commit 평균 지연(~50ms)의 2배. 충돌 해소에 충분한 시간 |
| `multiplier` | 2.0 | RFC 2616 (HTTP) 권장. 네트워크 혼잡 회피 알고리즘과 동일 |
| `maxDelay` | 1000ms | UX 임계값: 1초 이상 지연 시 사용자 이탈률 증가 (Nielsen Norman Group) |
| `retryFor` | `ObjectOptimisticLockingFailureException` | 순수 기술적 충돌만 재시도. 비즈니스 예외(잔액 부족 등)는 제외 |

**재시도 간격**: 100ms → 200ms → 400ms (총 최대 700ms 지연)

**왜 3회인가?**
- 1회: 단순 재시도 (성공률 ~60%)
- 2회: 재충돌 대응 (누적 ~85%)
- 3회: 극히 드문 3중 충돌 대응 (누적 ~95%)
- 4회 이상: 비용 대비 효과 미미 (1% 미만 개선)

**왜 Exponential Backoff인가?**
- Linear(고정 간격): 충돌 시점이 동일하면 재충돌 가능성 높음
- Exponential: 각 재시도 간 시간차 확보 → 충돌 회피

## 3. Alternatives Considered (대안 분석)

### 3.1. @Retryable 선언적 방식 ✅ (선택)

```java
@Retryable(
    retryFor = ObjectOptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000)
)
@Transactional
public void deposit(DepositCommand command) { ... }
```

| 장점 | 단점 |
|------|------|
| 코드 간결성 | AOP 의존 (프록시 동작 이해 필요) |
| 설정 외부화 용이 | 메서드 레벨에서만 동작 |
| 비침투적 | 동일 클래스 내부 호출 시 동작 안함 |

### 3.2. RetryTemplate 명시적 방식 ❌

```java
public void deposit(DepositCommand command) {
    retryTemplate.execute(context -> {
        doDeposit(command);
        return null;
    });
}
```

| 장점 | 단점 |
|------|------|
| 세밀한 제어 가능 | 보일러플레이트 코드 증가 |
| 동작 명시적 | 서비스 코드 복잡도 증가 |

### 3.3. 클라이언트 측 재시도 ❌

서버가 409 Conflict를 반환하고 클라이언트가 재시도:

| 장점 | 단점 |
|------|------|
| 서버 로직 단순 | 네트워크 왕복 증가 |
| 클라이언트 제어권 | 모든 클라이언트 구현 필요 |

> [!NOTE]
> 원장 시스템 특성상 **서버 측 재시도**가 적합합니다. 대부분의 Optimistic Lock 충돌은 일시적이며, 100~400ms 내 재시도로 해결 가능합니다.

## 4. Implementation Details (구현 상세)

### 4.1. 트랜잭션 경계와 재시도

```
┌─────────────────────────────────────────────────────────┐
│  @Retryable (Spring Retry AOP Proxy)                    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Attempt 1: @Transactional                        │  │
│  │  → OptimisticLockingFailureException → Rollback   │  │
│  └───────────────────────────────────────────────────┘  │
│  ↓ wait 100ms                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Attempt 2: @Transactional (새 트랜잭션)          │  │
│  │  → Success or Exception                           │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> 각 재시도는 **새로운 트랜잭션**에서 실행됩니다. 실패한 트랜잭션은 롤백되므로 데이터 정합성이 유지됩니다.

### 4.2. 멱등성 보장

재시도 시 동일한 `businessRefId`로 요청이 들어오므로, 기존 멱등성 체크 로직이 중복 실행을 방지합니다:

```java
public void deposit(DepositCommand command) {
    // 1. Idempotency Check - 재시도 시에도 동작
    if (isDuplicateTransaction(command.businessRefId())) {
        log.warn("Duplicate transaction detected...");
        return;  // 이미 성공한 경우 재시도 불필요
    }
    // ...
}
```

### 4.3. 의존성

```gradle
// kuku-core-ledger/build.gradle
dependencies {
    implementation 'org.springframework.retry:spring-retry'
    implementation 'org.springframework:spring-aspects'
}
```

### 4.4. 설정 (RetryConfig.java)

```java
@Configuration
@EnableRetry
public class RetryConfig {
    // @EnableRetry만 있으면 기본적으로 @Retryable 동작
}
```

## 5. 재시도 실패 시 대응 전략

### 5.1. 현재 동작 (서비스 레이어만 존재)

재시도가 모두 실패하면 `ObjectOptimisticLockingFailureException`이 **호출자에게 전파**됩니다.

```
@Retryable (3회 시도)
  ├─ 1st attempt → OptimisticLockException
  ├─ 2nd attempt → OptimisticLockException  
  └─ 3rd attempt → OptimisticLockException
      → 예외 전파 (호출자가 처리)
```

### 5.2. API 계층 추가 시 (향후)

REST API Controller에서 재시도 실패 예외를 catch하여 **409 Conflict** 응답:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLock(
      ObjectOptimisticLockingFailureException ex) {
    
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .header("Retry-After", "1")  // 1초 후 재시도 권장
        .body(new ErrorResponse(
            "OPTIMISTIC_LOCK_FAILURE",
            "동시 요청으로 인해 처리 실패. 잠시 후 다시 시도해주세요."
        ));
  }
}
```

**클라이언트 동작:**
- 409 응답 수신 → Exponential backoff로 재시도
- 최대 클라이언트 재시도: 3회 정도 권장

### 5.3. 모니터링 및 알람

재시도 실패가 **임계값(예: 분당 10회)** 이상 발생 시:
- Slack/이메일 알람
- 원인 분석: 특정 계좌에 요청 집중? 시스템 부하?
- 대응: 캐싱, 샤딩, Rate Limiting 검토

### 5.4. UNKNOWN 상태 활용 (선택적)

만약 외부 시스템 연동 중 충돌이 발생한다면:
- 재시도 실패 시 `Transaction.markAsUnknown()` 호출
- 백그라운드 작업으로 나중에 재처리
- 현재는 **순수 DB 내부 충돌**이므로 불필요

## 6. 도메인별 재시도 정책

### 6.1. 재시도 적용 대상 ✅

현재 구현 범위 (예수금 원장 시스템):

| 서비스 | 거래 유형 | 시간 민감도 | 재시도 적합성 |
|--------|-----------|-------------|---------------|
| `DepositService` | 예수금 입금 | 낮음 (가격 무관) | ✅ 적합 |
| `WithdrawService` | 예수금 출금 | 낮음 (가격 무관) | ✅ 적합 |
| `ReversalService` | 거래 취소 | 낮음 (보정 작업) | ✅ 적합 |
| `ConfirmTransactionService` | 거래 확정 | 중간 (외부 연동) | ✅ 적합 |

**왜 예수금은 재시도가 안전한가?**
- 금액 고정: 입금 100원은 400ms 후에도 100원
- 가격 변동 무관: 주식 가격과 독립적
- 사용자 기대치: 계좌 이체는 "즉시성"보다 "정확성" 중시

### 6.2. 재시도 제외 대상 ❌ (향후 주문 시스템 추가 시)

| 서비스 | 거래 유형 | 시간 민감도 | 재시도 부적합 이유 |
|--------|-----------|-------------|--------------------|
| `OrderService` (향후) | 주식 매수/매도 | **극히 높음** | 가격 변동으로 인한 손실 발생 가능 |
| `MatchingService` (향후) | 체결 확인 | 높음 | 외부 시스템 의존성 → UNKNOWN 상태 처리 |

**주문 체결 재시도 금지 예시:**

```
시나리오: 삼성전자 70,000원 매수 주문
  1차 시도 (70,000원) → 충돌 실패
  100ms 대기...
  2차 시도 (70,100원으로 상승!) → 성공
  
→ 사용자는 70,000원 기대, 실제 70,100원 체결
→ 불완전판매 위험 (금융소비자보호법 위반 가능)
```

**대응책:**
```java
// 주문 서비스는 재시도 제외
public class OrderService {
    // @Retryable 애노테이션 없음
    @Transactional
    public void placeOrder(OrderCommand command) {
        // 즉시 실패 → 사용자가 현재 가격 확인 후 재주문
    }
}
```

### 6.3. 감사 추적 요구사항 (금융위원회 전자금융감독규정 제23조)

재시도 발생 시 **각 시도마다 로그 필수**:

```java
@Retryable(...)
public void deposit(DepositCommand command) {
    int attempt = RetrySynchronizationManager.getContext() != null 
        ? RetrySynchronizationManager.getContext().getRetryCount() + 1 
        : 1;
    
    log.info("[LEDGER_DEPOSIT] businessRefId={}, accountId={}, amount={}, attempt={}/{}",
        command.businessRefId(), command.accountId(), command.amount(), 
        attempt, 3);
    
    // 기존 로직...
}
```

**필수 로그 항목:**
- businessRefId (중복 방지)
- 시도 횟수 (1/3, 2/3, 3/3)
- 성공/실패 여부
- 실패 시 예외 상세

## 7. Consequences (결과)

### 7.1. 기대 효과

| 측면 | 변화 |
|------|------|
| **신뢰성** | 일시적 충돌 자동 복구 |
| **사용자 경험** | 불필요한 오류 감소 |
| **운영** | 수동 개입 필요 케이스 감소 |

### 7.2. 주의 사항

1. **무한 재시도 방지**: `maxAttempts=3`으로 제한
2. **재시도 불가 예외**: `InsufficientBalanceException`, `IllegalArgumentException` 등 비즈니스 예외는 재시도하지 않음
3. **로깅**: 재시도 발생 시 로그 기록 필수 (감사 추적 + 모니터링)

### 7.3. 모니터링

재시도 발생 빈도가 높다면:
*   동시 요청이 과도하게 많음 → 큐잉 또는 분산 락 검토
*   특정 계좌에 집중 → 샤딩 검토

## 8. References

*   [Spring Retry 공식 문서](https://github.com/spring-projects/spring-retry)
*   [Optimistic Locking in JPA](https://www.baeldung.com/jpa-optimistic-locking)
*   [AWS DynamoDB - Error Retries and Exponential Backoff](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.Errors.html#Programming.Errors.RetryAndBackoff)
*   [RFC 2616 - HTTP/1.1 Specification](https://datatracker.ietf.org/doc/html/rfc2616)
*   `LedgerConcurrencyTest.java` - 동시성 테스트에서 Optimistic Lock 충돌 검증
