# [ADR-011] Global Exception Handling 전략

*   **Status**: Accepted
*   **Date**: 2026-01-08
*   **Author**: dding94

## 1. Context (배경)

### 1.1. 문제 상황

MSA 기반 시스템에서 각 모듈(Order, Ledger, Portfolio 등)이 독립적으로 예외를 정의하고 처리할 때 다음 문제가 발생합니다:

| 문제 | 영향 |
|------|------|
| 일관성 없는 에러 응답 형식 | 클라이언트가 각 서비스별로 다른 에러 파싱 로직 필요 |
| 중복된 Exception Handler 코드 | 유지보수 비용 증가 |
| HTTP Status 매핑 산재 | Handler마다 동일 예외에 다른 상태 코드 반환 가능 |
| 장애 추적 어려움 | 로그와 응답 간 연결고리 부재 |

### 1.2. 요구사항

1. **모듈 독립성**: 각 도메인이 자체 에러 코드를 정의할 수 있어야 함
2. **일관된 응답 형식**: 클라이언트가 예측 가능한 에러 응답 구조
3. **확장성**: 새 도메인(Portfolio, MarketData) 추가 시 최소 변경
4. **관측성**: 장애 추적을 위한 trackingId 포함

## 2. Decision (결정)

**공통 모듈(`kuku-common`)에 예외 처리 프레임워크를 구축**하고, 각 도메인이 이를 확장하여 사용합니다.

### 2.1. 핵심 구성요소

```
kuku-common/
└── exception/
    ├── ErrorCode.java          # 에러 코드 인터페이스
    ├── CommonErrorCode.java    # 공통 에러 코드 (500, 400 등)
    ├── BusinessException.java  # 비즈니스 예외 추상 클래스
    └── ErrorResponse.java      # 표준 에러 응답 DTO

kuku-order-system/
└── domain/
    └── exception/              # 도메인 예외 서브패키지
        ├── OrderErrorCode.java
        ├── OrderNotFoundException.java
        ├── InvalidOrderStateException.java
        ├── InvalidOrderSideException.java
        ├── InvalidOrderTypeException.java
        ├── OrderLimitExceededException.java
        └── OrderValidationException.java

kuku-core-ledger/
└── domain/
    └── exception/              # 도메인 예외 서브패키지
        ├── LedgerErrorCode.java
        ├── InsufficientBalanceException.java
        └── InvalidTransactionStateException.java
```

### 2.2. ErrorCode 인터페이스

```java
public interface ErrorCode {
    String getCode();      // "ORDER_001", "LEDGER_001"
    String getMessage();   // 기본 메시지
    int getStatus();       // HTTP Status Code
}
```

**설계 결정:**
- `code`: 도메인 Prefix로 에러 출처 즉시 파악 (ORDER_, LEDGER_, COMMON_)
- `status`: ErrorCode에 HTTP Status 포함 → Handler 단순화

### 2.3. 도메인별 ErrorCode Enum

```java
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER_001", "Order not found", 404),
    INVALID_ORDER_STATE("ORDER_002", "Invalid order state", 409),
    ORDER_VALIDATION_FAILED("ORDER_003", "Order validation failed", 422),
    ORDER_LIMIT_EXCEEDED("ORDER_004", "Order limit exceeded", 422),
    INVALID_ORDER_SIDE("ORDER_005", "Invalid order side", 400),
    INVALID_ORDER_TYPE("ORDER_006", "Invalid order type", 400);
    // ...
}
```

### 2.4. BusinessException 추상 클래스

```java
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    
    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

**왜 RuntimeException을 직접 상속하지 않고 BusinessException 중간 계층을 두는가?**

| 관점 | RuntimeException 직접 상속 | BusinessException 상속 |
|------|--------------------------|----------------------|
| **Handler 코드** | 예외마다 별도 핸들러 필요 | 단일 핸들러로 모든 비즈니스 예외 처리 |
| **계약 강제** | `getErrorCode()` 보장 없음 | 모든 하위 예외에 `ErrorCode` 강제 |
| **구분 가능성** | 시스템/비즈니스 예외 구분 불가 | catch 블록에서 타입으로 명확히 구분 |
| **확장성** | 예외 추가 시 Handler 추가 필요 | 예외 클래스만 추가하면 됨 |

**핵심 이점:**

1. **단일 ExceptionHandler**: `@ExceptionHandler(BusinessException.class)`로 모든 비즈니스 예외 처리
2. **공통 계약(Contract)**: 모든 하위 예외가 `getErrorCode()`를 반드시 구현 → 일관된 응답 생성
3. **시스템 vs 비즈니스 분리**: 비즈니스 규칙 위반(잔액 부족)과 시스템 장애(DB 연결 실패)를 타입으로 구분
4. **AOP/메트릭 적용**: 비즈니스 예외만 선택적으로 모니터링 대시보드에 집계 가능

### 2.5. 표준 ErrorResponse

```java
public class ErrorResponse {
    private final String code;        // "ORDER_001"
    private final String message;     // "Order not found: 12345"
    private final int status;         // 404
    private final Instant timestamp;
    private final String trackingId;  // 장애 추적용
}
```

## 3. Alternatives Considered (대안 분석)

### 3.1. 단일 GlobalErrorCode Enum ❌

```java
// 모든 에러 코드를 하나의 Enum에 정의
public enum GlobalErrorCode implements ErrorCode {
    ORDER_NOT_FOUND, LEDGER_INSUFFICIENT_BALANCE, PORTFOLIO_NOT_FOUND...
}
```

| 장점 | 단점 |
|------|------|
| 한 곳에서 관리 | MSA에서 모듈 간 의존성 증가 |
| 중복 코드 방지 | 변경 시 전체 빌드 필요 |
|  | 팀 간 병렬 개발 충돌 |

**기각 이유**: MSA 원칙 위반. 각 서비스가 독립 배포 불가.

### 3.2. String 상수 기반 에러 코드 ❌

```java
throw new BusinessException("ORDER_001", "Order not found");
```

| 장점 | 단점 |
|------|------|
| 매우 단순 | 타입 안전성 없음 |
|  | IDE 자동완성 불가 |
|  | 오타 런타임에서 발견 |
|  | 에러 코드 목록 파악 어려움 |

**기각 이유**: 금융 시스템에서 런타임 에러는 위험.

### 3.3. 계층적 예외 구조 ❌

```java
BusinessException
├── OrderException
│   ├── OrderNotFoundException
│   └── InvalidOrderStateException
└── LedgerException
    ├── InsufficientBalanceException
    └── InvalidTransactionStateException
```

| 장점 | 단점 |
|------|------|
| 세분화된 catch 가능 | 과도한 계층 복잡도 |
| 도메인별 그룹화 | 중간 클래스가 무의미 |
|  | Handler 코드 증가 |

**기각 이유**: 중간 계층(OrderException)이 실질적 역할 없음. ErrorCode로 충분히 구분 가능.

### 3.4. Spring ResponseEntityExceptionHandler 상속 ❌

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(...) { ... }
}
```

| 장점 | 단점 |
|------|------|
| Spring 표준 방식 | 커스텀 응답 형식 제한적 |
| 기본 예외 처리 내장 | ErrorResponse 포맷 변경 어려움 |
|  | protected 메서드 의존 |

**기각 이유**: `ErrorResponse` 포맷(trackingId, code)을 자유롭게 정의하기 어려움.

## 4. Implementation Details (구현 상세)

### 4.1. ExceptionHandler 처리 흐름

```
Client Request
     ↓
Controller → throws BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
     ↓
@RestControllerAdvice (OrderExceptionHandler)
     ↓
┌─────────────────────────────────────────────────────────────┐
│ @ExceptionHandler(BusinessException.class)                  │
│                                                             │
│ 1. ErrorCode 추출: ex.getErrorCode()                        │
│ 2. trackingId 생성: MDC.get("traceId") or UUID              │
│ 3. ErrorResponse 생성                                       │
│ 4. HTTP Status: ex.getErrorCode().getStatus()               │
└─────────────────────────────────────────────────────────────┘
     ↓
ErrorResponse JSON
{
    "code": "ORDER_001",
    "message": "Order not found: 12345",
    "status": 404,
    "timestamp": "2026-01-08T12:00:00Z",
    "trackingId": "abc-123-def"
}
```

### 4.2. 도메인 예외에 진단 정보 포함

```java
public class InsufficientBalanceException extends BusinessException {
    private final Long accountId;
    private final BigDecimal requested;
    private final BigDecimal available;
    
    // 장애 발생 시 즉시 컨텍스트 파악 가능
}
```

**이점:**
- 로그 분석 시 메시지 파싱 불필요
- 모니터링 시스템과 타입 안전한 연동
- 금융 감독 규정 충족 (거래 실패 사유 명확화)

### 4.3. Unchecked Exception 선택 이유

| Checked Exception | Unchecked Exception |
|-------------------|---------------------|
| 모든 호출부에 throws 강제 | throws 불요 |
| 서비스 레이어 오염 | 깔끔한 시그니처 |
| @Transactional 롤백 기본 X | @Transactional 롤백 기본 O |

> [!NOTE]
> Spring의 `@Transactional`은 기본적으로 **RuntimeException**만 롤백합니다.
> 비즈니스 예외는 대부분 롤백이 필요하므로 Unchecked가 적합합니다.

## 5. Consequences (결과)

### 5.1. 기대 효과

| 측면 | 개선 |
|------|------|
| **확장성** | 새 도메인 추가 시 ErrorCode Enum만 생성 |
| **일관성** | 모든 서비스가 동일한 에러 응답 형식 |
| **유지보수** | 단일 Handler로 모든 비즈니스 예외 처리 |
| **관측성** | trackingId로 로그-응답 연결 |
| **타입 안전** | 컴파일 타임에 에러 코드 검증 |

### 5.2. 트레이드오프

| 비용 | 설명 |
|------|------|
| 초기 설계 비용 | common 모듈 구축 필요 |
| 학습 곡선 | 새 개발자가 패턴 숙지 필요 |
| 모듈 의존성 | 모든 서비스가 common 의존 |

### 5.3. 향후 고려사항

1. **i18n 지원**: ErrorCode에 메시지 키 추가하여 다국어 대응
2. **에러 카탈로그 API**: 클라이언트용 에러 코드 목록 조회 API
3. **메트릭 연동**: 에러 코드별 발생 빈도 Grafana 대시보드

## 6. References

*   [Zalando RESTful API Guidelines - Error Handling](https://opensource.zalando.com/restful-api-guidelines/#176)
*   [RFC 7807 - Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807)
*   [Spring Exception Handling Best Practices](https://www.baeldung.com/exception-handling-for-rest-with-spring)
*   `POLICY.md` - 프로젝트 글로벌 정책
