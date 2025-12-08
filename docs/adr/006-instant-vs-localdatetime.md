# [ADR-006] 시간 객체 표준화: Instant vs LocalDateTime

*   **Status**: Accepted
*   **Date**: 2025-12-08
*   **Author**: dding94

## 1. Context (배경)

금융 시스템에서 **시간 정보는 감사 추적(Audit Trail), 규제 준수, 거래 순서 보장**의 핵심입니다. 잘못된 시간 표현은 다음과 같은 심각한 문제를 야기할 수 있습니다:

*   분산 시스템 간 거래 순서 불일치
*   일광절약시간(DST) 전환 시 데이터 손실 또는 중복
*   글로벌 운영 시 타임존 해석 충돌

Java 8+에서 시간을 표현하는 두 가지 주요 클래스가 있습니다:
*   `java.time.Instant`: UTC 기준의 절대 시간 (타임스탬프)
*   `java.time.LocalDateTime`: 타임존 없는 로컬 시간

우리는 **도메인 엔티티와 데이터베이스에 저장할 시간 타입**에 대한 표준을 수립해야 합니다.

## 2. Decision (결정)

1.  **Domain Entities & Database**: **`Instant`를 사용한다.**
    *   모든 도메인 엔티티의 시간 필드(`createdAt`, `updatedAt`, `executedAt` 등)는 `Instant` 타입으로 선언한다.
    *   데이터베이스에는 UTC 기준으로 저장한다 (MySQL: `DATETIME(6)` 또는 `TIMESTAMP`).
2.  **Presentation Layer**: **사용자 표시 시에만 `LocalDateTime`으로 변환한다.**
    *   API 응답 시 클라이언트의 타임존을 고려하여 변환하거나, ISO-8601 문자열(`2024-01-01T10:00:00Z`)로 반환한다.
3.  **Clock Injection**: **`java.time.Clock`을 주입받아 사용한다.**
    *   서비스에서 `Instant.now(clock)` 형태로 시간을 획득하여 테스트 가능성(Deterministic Testing)을 확보한다.

## 3. Detailed Analysis (상세 분석)

### 3.1. LocalDateTime의 문제점

| 문제 | 설명 | 위험도 |
|------|------|--------|
| **타임존 모호성** | `2024-01-01T10:00:00`이 어느 나라의 10시인지 알 수 없음 | 🔴 Critical |
| **DST 충돌** | 서머타임 전환 시 같은 시간이 2번 존재하거나 건너뜀 | 🔴 Critical |
| **분산 시스템 불일치** | 서버마다 다른 타임존 설정 시 시간 해석이 달라짐 | 🟠 High |

#### DST 문제 예시 (미국 동부 시간)
```
2024-03-10 02:30 → 존재하지 않는 시간 (02:00 → 03:00 건너뜀)
2024-11-03 01:30 → 2번 발생하는 시간 (01:00~02:00 반복)
```

금융 거래에서 이런 모호함은 **규제 위반** 또는 **거래 분쟁**으로 이어질 수 있습니다.

### 3.2. Instant의 장점

| 장점 | 설명 |
|------|------|
| **전 세계 유일한 시점** | UTC 기준으로 해석 여지 없음 |
| **DST 면역** | 절대 시간이므로 DST 영향 없음 |
| **정렬 용이** | 단순 숫자 비교로 시간 순서 보장 |
| **ISO-8601 호환** | `2024-01-01T01:00:00Z` 형태로 직렬화 |

### 3.3. POLICY.md 정합성

`docs/POLICY.md` 64번 라인의 **Deterministic Testing** 정책:
> `LocalDateTime.now()`, `Random` 등을 직접 사용하지 말고, **고정된 값(Fixed Value)**을 주입하거나 Mocking하여 테스트하세요.

`Clock` 주입과 `Instant` 사용은 이 정책을 자연스럽게 준수합니다:

```java
@Service
public class DepositService {
    private final Clock clock;
    
    public void deposit(DepositCommand command) {
        Instant now = clock.instant();  // Deterministic
        Transaction tx = Transaction.createDeposit(..., now);
    }
}
```

## 4. Implementation Example (구현 예시)

### 4.1. Domain Entity

```java
public class Transaction {
    private final Long id;
    private final Instant createdAt;  // Instant 사용
    
    public static Transaction createDeposit(String description, Instant createdAt) {
        return new Transaction(null, description, createdAt);
    }
}
```

### 4.2. Presentation Layer (API Response)

```java
public record TransactionResponse(
    Long id,
    String createdAt  // ISO-8601 문자열로 반환
) {
    public static TransactionResponse from(Transaction tx, ZoneId userZone) {
        return new TransactionResponse(
            tx.getId(),
            tx.getCreatedAt().atZone(userZone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }
}
```

### 4.3. JPA Entity Mapping

```java
@Entity
public class TransactionJpaEntity {
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;  // JPA 2.2+에서 Instant 직접 지원
}
```

### 4.4. Database Schema

```sql
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL  -- 마이크로초 정밀도
);
```

## 5. Consequences (결과 및 대응)

### 5.1. 마이그레이션 필요 시

현재 `LocalDateTime`을 사용 중인 경우:
1.  도메인 엔티티의 타입을 `Instant`로 변경
2.  JPA Converter 또는 직접 매핑 수정
3.  기존 데이터는 UTC로 변환하여 저장

### 5.2. 테스트 작성

```java
class TransactionTest {
    private static final Instant FIXED_TIME = 
        Instant.parse("2024-01-01T00:00:00Z");
    
    @Test
    void shouldCreateTransactionWithFixedTime() {
        Transaction tx = Transaction.createDeposit("Deposit", FIXED_TIME);
        assertThat(tx.getCreatedAt()).isEqualTo(FIXED_TIME);
    }
}
```

## 6. Technical Refinements (기술적 보완 사항)

### 6.1. MySQL 데이터 타입: DATETIME(6) 권장

| 타입 | 범위 | 문제점 |
|------|------|--------|
| `TIMESTAMP` | 1970 ~ **2038-01-19** | 🔴 **2038년 문제 (Year 2038 Problem)** |
| `DATETIME(6)` | 1000 ~ 9999 | ✅ 장기 금융 상품, 만기일 저장 가능 |

> [!CAUTION]
> MySQL의 `TIMESTAMP` 타입은 내부적으로 32비트 Unix 시간을 사용하므로 **2038년 1월 19일 이후의 날짜를 저장할 수 없습니다.** 장기 채권, 보험, 연금 등의 만기일 저장 시 심각한 문제가 발생할 수 있습니다.

**권장 설정:**
```sql
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,  -- TIMESTAMP 대신 DATETIME(6) 사용
    -- ...
);
```

### 6.2. Spring Boot UTC 강제 설정

애플리케이션 레벨에서 **항상 UTC로 변환**하여 저장하도록 JDBC 드라이버 설정을 추가합니다:

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC  # JDBC 레벨에서 강제 UTC 변환
```

이 설정을 통해 개발자 실수로 인한 타임존 불일치를 방지할 수 있습니다.

### 6.3. JSON 직렬화 포맷 (ISO-8601)

Spring Boot의 Jackson 기본 설정에서 `Instant`는 **Decimal(초 단위 숫자)**로 직렬화될 수 있습니다.

**문제:**
```json
// 기본 설정 (읽기 어려움)
{ "createdAt": 1704067200.000000000 }

// 원하는 형태 (ISO-8601)
{ "createdAt": "2024-01-01T00:00:00Z" }
```

**해결책 (application.yml):**
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false  # ISO-8601 문자열로 직렬화
    deserialization:
      adjust-dates-to-context-time-zone: false  # 역직렬화 시 타임존 조정 비활성화
```

또는 엔티티/DTO 필드에 명시적 어노테이션:
```java
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
private Instant createdAt;
```

## 7. Conclusion (결론)

금융 시스템에서 **시간의 정확성**은 필수입니다. `LocalDateTime`은 타임존 정보 부재로 인해 해석 모호성이 존재하며, DST 문제에 취약합니다.

따라서 우리는 **모든 도메인 시간 필드에 `Instant`를 사용**하고, 사용자 표시 시에만 적절한 타임존으로 변환하는 전략을 채택합니다.

**핵심 기술 스택:**
| 계층 | 선택 |
|------|------|
| Java 도메인 | `Instant` |
| MySQL 스키마 | `DATETIME(6)` (TIMESTAMP ❌) |
| Hibernate 설정 | `hibernate.jdbc.time_zone: UTC` |
| Jackson 직렬화 | `write-dates-as-timestamps: false` |

이는 **글로벌 운영**, **규제 준수**, **정확한 감사 추적**, 그리고 **2038년 이후 장기 금융 상품 지원**을 위한 필수 요건입니다.
