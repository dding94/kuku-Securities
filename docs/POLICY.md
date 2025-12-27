# Global Development Policy & Guidelines

이 문서는 프로젝트의 일관성, 유지보수성, 그리고 높은 품질을 유지하기 위한 글로벌 정책과 가이드라인을 정의합니다. 모든 기여자는 이 문서를 숙지하고 준수해야 합니다.

---

## 1. Git Strategy (브랜치 전략)

**GitHub Flow**를 따릅니다. 이는 단순하고 가벼운 브랜치 전략으로, 지속적인 배포(CD)에 최적화되어 있습니다.

### Branch Structure
*   **`main`**: 언제나 배포 가능한 상태(Production-ready)를 유지합니다. **절대 직접 커밋하지 않습니다.**
*   **Feature Branches**: 새로운 기능이나 버그 수정은 `main`에서 브랜치를 생성하여 작업합니다.
    *   Naming: `feature/issue-number-description` (e.g., `feature/12-order-api`)
    *   Naming: `fix/issue-number-description` (e.g., `fix/15-login-error`)

### Workflow
1.  `main` 브랜치에서 새로운 브랜치를 생성합니다.
2.  로컬에서 작업하고 커밋합니다.
3.  원격 저장소에 푸시하고 **Pull Request (PR)**를 생성합니다.
4.  코드 리뷰와 논의를 진행합니다.
5.  리뷰가 완료되고 테스트가 통과하면 `main`으로 병합(Merge)합니다.
6.  `main` 브랜치가 업데이트되면 즉시 배포될 수 있습니다.

### Pull Request Guidelines (PR Size & Scope)
*   **Small & Focused**: 하나의 PR은 **하나의 논리적 변경(One Logical Change)**만 담아야 합니다. (기능 구현과 리팩토링을 섞지 마세요.)
*   **Size Limit**: **200~400 Lines of Code (LOC)** 이내를 권장합니다.
*   **Reviewability**: 리뷰어가 **15분 이내**에 파악할 수 있는 크기여야 합니다. 변경이 너무 크다면 Feature Toggle을 사용하거나 하위 작업으로 쪼개서 PR을 올리세요.

### Commit Convention (Conventional Commits)
커밋 메시지는 다음 형식을 따릅니다:
`type(scope): subject`

*   **feat**: 새로운 기능 추가
*   **fix**: 버그 수정
*   **docs**: 문서 수정
*   **style**: 코드 포맷팅, 세미콜론 누락 등 (로직 변경 없음)
*   **refactor**: 코드 리팩토링 (기능 변경 없음)
*   **test**: 테스트 코드 추가/수정
*   **chore**: 빌드 업무 수정, 패키지 매니저 설정 등

---

## 2. Coding Standards (코드 작성 원칙)

### SOLID Principles
*   **SRP (Single Responsibility Principle)**: 클래스와 함수는 단 하나의 책임만 가져야 합니다. 변경의 이유는 오직 하나여야 합니다.
*   **OCP (Open/Closed Principle)**: 확장에는 열려 있고, 수정에는 닫혀 있어야 합니다. 인터페이스와 다형성을 적극 활용합니다.
*   **LSP (Liskov Substitution Principle)**: 서브 타입은 언제나 기반 타입으로 교체할 수 있어야 합니다.
*   **ISP (Interface Segregation Principle)**: 범용 인터페이스 하나보다 구체적인 여러 개의 인터페이스가 낫습니다.
*   **DIP (Dependency Inversion Principle)**: 구체화에 의존하지 말고 추상화에 의존해야 합니다.

### Clean Code
*   **Naming**: 변수, 함수, 클래스 이름은 그 의도를 명확히 드러내야 합니다. 축약어 사용을 지양합니다.
*   **Functions**: 함수는 작게 만들고, 한 가지 일만 하도록 합니다.
*   **Comments**: 코드로 의도를 표현할 수 없을 때만 주석을 작성합니다. "무엇"이 아닌 "왜"를 설명합니다.
*   **Abstraction Levels**: 코드를 작성할 때 추상화 레벨을 맞춥니다. 한 함수 안에서는 동일한 수준의 추상화만 존재해야 합니다. (SLAP: Single Level of Abstraction Principle)

### Testing

#### 기본 원칙
*   **TDD (Test-Driven Development)**: 가능한 경우 테스트를 먼저 작성하고 구현합니다.
*   **Coverage**: 도메인 로직에 대해서는 높은 테스트 커버리지를 유지합니다.
*   **Test Pyramid**: Unit Test > Integration Test > E2E Test 비율을 유지합니다.

#### 결정론적 테스트 (Deterministic Testing)
테스트는 언제 실행해도 동일한 결과를 보장해야 합니다.
*   `LocalDateTime.now()`, `Instant.now()`, `Random` 등을 직접 사용하지 말고, **고정된 값(Fixed Value)**을 주입하거나 Mocking하여 테스트하세요.

```java
// ✅ 권장: 고정 시간 상수 사용
class TransactionTest {
    private static final Instant FIXED_TIME = Instant.parse("2025-01-01T03:00:00Z");
    
    @Test
    void createTransaction_success() {
        Transaction tx = new Transaction(..., FIXED_TIME);
        assertThat(tx.getCreatedAt()).isEqualTo(FIXED_TIME);
    }
}
```

#### 테스트 구조화 (@Nested)
테스트가 많아지면 flat한 구조는 관리가 어렵습니다. **`@Nested`**를 사용하여 논리적 그룹으로 구조화합니다.

```java
// ❌ Anti-Pattern: Flat 구조
class TransactionTest {
    void createTransaction_throwsException_whenTypeIsNull() {}
    void createTransaction_throwsException_whenStatusIsNull() {}
    void toReversed_returnsNewTransaction() {}
    void toReversed_throwsException_whenNotPosted() {}
}

// ✅ Best Practice: @Nested로 그룹화
class TransactionTest {
    @Nested
    @DisplayName("생성자 유효성 검증")
    class ConstructorValidation {
        @Test void throwsException_whenTypeIsNull() {}
        @Test void throwsException_whenStatusIsNull() {}
    }
    
    @Nested
    @DisplayName("toReversed() 메서드")
    class ToReversed {
        @Test void success_whenStatusIsPosted() {}
        @Test void throwsException_whenStatusIsNotPosted() {}
    }
}
```

#### 파라미터화 테스트 (@ParameterizedTest)
유사한 패턴의 테스트가 반복되면 **`@ParameterizedTest`**를 활용하여 중복을 제거합니다.

```java
// ❌ Anti-Pattern: 유사 테스트 반복
@Test void toReversed_throwsException_whenStatusIsPending() {}
@Test void toReversed_throwsException_whenStatusIsReversed() {}
@Test void toReversed_throwsException_whenStatusIsUnknown() {}

// ✅ Best Practice: @ParameterizedTest
@ParameterizedTest
@EnumSource(value = TransactionStatus.class, names = {"PENDING", "REVERSED", "UNKNOWN"})
@DisplayName("POSTED가 아닌 상태는 역분개 불가")
void throwsException_whenStatusIsNotPosted(TransactionStatus status) {
    Transaction tx = createTransaction(status);
    
    assertThatThrownBy(tx::toReversed)
        .isInstanceOf(InvalidTransactionStateException.class);
}
```

#### 경계값 및 null 허용 테스트
의도적으로 `null`을 허용하는 필드는 테스트로 문서화하여 **의도를 명확히** 합니다.

```java
@Test
@DisplayName("description은 null을 허용한다")
void allowsNullDescription() {
    Transaction tx = new Transaction(1L, TransactionType.DEPOSIT, null, ...);
    assertThat(tx.getDescription()).isNull();
}
```

#### 테스트 메서드 네이밍 규칙
일관된 네이밍 패턴을 사용합니다: **`methodName_expectedResult_condition`** 또는 **`expectedResult_condition`** (@Nested 내부)

| 패턴 | 예시 |
|------|------|
| Top-level | `createTransaction_throwsException_whenTypeIsNull()` |
| @Nested 내부 | `throwsException_whenTypeIsNull()` (메서드명은 클래스명에서 유추) |
| 성공 케이스 | `success_whenAllFieldsValid()` |

#### 테스트 헬퍼 메서드
테스트 픽스처 생성 로직의 중복을 제거하기 위해 **헬퍼 메서드**를 사용합니다.

```java
class TransactionTest {
    private static final Instant FIXED_TIME = Instant.parse("2025-01-01T03:00:00Z");

    // 테스트 픽스처 헬퍼
    private Transaction createTransaction(TransactionStatus status) {
        return new Transaction(1L, TransactionType.DEPOSIT, "Test", "REF-001",
                status, null, FIXED_TIME);
    }
}
```

#### AssertJ 사용
가독성 높은 검증을 위해 **AssertJ**를 사용합니다.

```java
// ✅ 권장: AssertJ fluent assertions
assertThat(result).isNotSameAs(original);
assertThat(result.getStatus()).isEqualTo(TransactionStatus.REVERSED);
assertThatThrownBy(() -> tx.toReversed())
    .isInstanceOf(InvalidTransactionStateException.class)
    .hasMessageContaining("PENDING");
```

### Class Member Ordering (클래스 멤버 순서)

클래스 내부 멤버는 다음 순서로 정렬합니다:

1. **상수 (static final)**: `private static final`, `public static final`
2. **정적 필드 (static)**: `private static`, `public static`
3. **인스턴스 필드**: `private`, `protected`, `public`
4. **생성자**: 기본 생성자, 파라미터 생성자 순
5. **정적 팩토리 메서드**: `public static` (생성 관련)
6. **공개 메서드 (public)**: 비즈니스 로직
7. **패키지/보호된 메서드**: `protected`, package-private
8. **비공개 메서드 (private)**: 내부 헬퍼 메서드

```java
public class Example {
    // 1. 상수
    private static final String CONSTANT = "value";

    // 2. 정적 필드
    private static int counter;

    // 3. 인스턴스 필드
    private final Long id;
    private String name;

    // 4. 생성자
    public Example(Long id, String name) { ... }

    // 5. 정적 팩토리 메서드
    public static Example create(String name) { ... }

    // 6. 공개 메서드
    public void doSomething() { ... }

    // 7. 비공개 메서드
    private void validate() { ... }
}
```

---

## 3. Design Principles (설계 원칙)

### Cohesion & Coupling (응집도와 결합도)
*   **High Cohesion (높은 응집도)**: 관련된 기능과 데이터는 한 곳(모듈, 클래스)에 모아둡니다. 함께 변경되는 것들은 함께 있어야 합니다.
*   **Low Coupling (낮은 결합도)**: 모듈 간의 의존성을 최소화합니다. 직접적인 참조보다는 인터페이스나 이벤트를 통해 느슨하게 결합합니다.

### Domain-Driven Design (DDD)
*   **Ubiquitous Language (보편 언어)**: 기획자, 개발자, 도메인 전문가가 동일한 용어를 사용합니다.
*   **Bounded Context**: 도메인의 경계를 명확히 하고, 각 컨텍스트 내에서 모델의 정합성을 유지합니다.

---

## 4. Architecture Overview (아키텍처)

### Microservices Architecture (MSA)
시스템은 비즈니스 도메인에 따라 독립적인 서비스로 분할됩니다.
*   **Core Ledger**: 원장 관리
*   **Order System**: 주문 처리
*   **Market Data**: 시세 처리

### Hexagonal Architecture (Ports & Adapters)
도메인 로직을 외부 세계(DB, UI, 외부 API)로부터 격리합니다.
*   **Domain**: 핵심 비즈니스 로직 (순수 Java 코드)
*   **Ports**: 도메인이 외부와 소통하기 위한 인터페이스 (Inbound/Outbound)
*   **Adapters**: 포트의 구현체 (Controller, JPA Repository, Feign Client)
    *   **Inbound Ports (UseCase)**:
        *   **Commands (State Change)**: 상태를 변경하는 작업은 **Command 객체**를 사용하여 파라미터를 캡슐화합니다. (e.g., `DepositCommand`)
            *   Command 객체 생성 시 **Self-Validation**을 수행합니다.
        *   **Queries (Read Only)**: 데이터를 조회하는 작업은 **Query 객체**를 사용하거나, 단순한 경우 원시 타입을 사용합니다. (e.g., `GetHistoryQuery` vs `Long id`)
            *   조회 필터가 복잡하거나 파라미터가 2개 이상인 경우 `Query` 객체로 캡슐화합니다.

### Event-Driven Architecture
서비스 간의 결합도를 낮추기 위해 비동기 메시징(Kafka)을 적극 활용합니다.
*   **Eventual Consistency**: 분산 트랜잭션 대신 이벤트를 통한 결과적 일관성을 추구합니다.

### Outbound Port 설계 가이드라인

Port를 과도하게 세분화하면 보일러플레이트가 증가하고, 과도하게 통합하면 Fat Interface가 됩니다. 다음 기준을 따릅니다.

#### 통합형 Port 권장 상황 (기본 원칙)
*   동일 Aggregate에 대한 일반적인 CRUD/조회가 대부분인 경우
*   모두 같은 DB / 같은 트랜잭션 경계에서 처리되는 경우
*   CQRS나 다른 스토리지 분리가 당장 필요하지 않은 경우
*   프로젝트/팀 규모가 작고, 구조를 복잡하게 가져가고 싶지 않은 경우

```java
// 권장: 통합형 Port (5개 이하 메서드)
interface TransactionPort {
    Optional<Transaction> findById(Long id);
    Optional<Transaction> findByBusinessRefId(String refId);
    Transaction save(Transaction tx);
    void update(Transaction tx);
}
```

#### 세분화형 Port 고려 상황
1.  **읽기/쓰기 특성이 크게 다를 때** (CQRS, Reporting)
    *   쓰기: 트랜잭션 중요 (MySQL, 강한 일관성)
    *   읽기: 별도 Read DB, ElasticSearch, 캐시 등
    *   → `TransactionCommandPort` / `TransactionQueryPort` 분리
2.  **전혀 다른 외부 시스템을 붙일 예정일 때**
    *   외부 증권사 API, Kafka 이벤트 발행, Redis 캐시 등
    *   → 대상 시스템별 Port 분리
3.  **하나의 Port에 결이 다른 메서드가 섞이기 시작할 때**
    *   기본 조회, 통계/리포트용 조회, 배치용 복잡한 쿼리 등
    *   → `AccountQueryPort` / `AccountReportingPort` / `AccountBatchPort` 분리

---

## 5. Service Layer & Code Organization (서비스 계층 구조)

서비스 클래스의 비대화를 막고 유지보수성을 높이기 위해 다음 원칙을 따릅니다.
서비스가 커지면 개발자들은 코드를 찾기 위해 스크롤을 끝없이 내려야 하고, Git Merge 충돌도 빈번해집니다.

### 1단계: UseCase별 구현체 분리 (기본 원칙)
*   **One Interface, One Implementation**: 각 UseCase 인터페이스마다 별도의 구현 클래스(`XxxService`)를 만듭니다.
    *   e.g., `DepositUseCase` -> `DepositService`, `WithdrawUseCase` -> `WithdrawService`
*   `LedgerService`와 같은 포괄적인 이름은 지양합니다.

### 2단계: 공통 로직 추출 (Domain Service / Component)
*   여러 서비스에서 공통으로 사용되는 로직(유효성 검증, 복잡한 계산 등)은 **Domain Service**나 **Component**로 추출하여 재사용합니다.
*   Private 메서드 복사/붙여넣기를 금지합니다.

### 3단계: Facade 패턴 (복합 로직)
*   여러 UseCase를 묶어서 실행해야 하는 경우(e.g., 이체: 출금 + 입금)에만 상위 레벨의 **Facade Service**를 둡니다.
*   단순 UseCase 구현체끼리는 서로 직접 호출하지 않도록 주의합니다 (순환 참조 방지).

---

## 6. Domain Logic Encapsulation (도메인 로직 캡슐화)

서비스 계층에 분기 로직이 누적되면 코드가 비대해지고 확장성이 떨어집니다. 다음 원칙을 통해 도메인 엔티티에 로직을 응집시킵니다.

### Enum 기반 분기 로직은 도메인에 캡슐화

서비스에서 Enum 값에 따라 분기하는 `if-else` 또는 `switch` 문이 등장하면, 해당 로직을 **도메인 엔티티의 메서드**로 이동시킵니다.

#### ❌ Anti-Pattern: 서비스에 분기 로직
```java
// ReversalService.java
Balance restoredBalance;
if (entry.getEntryType() == EntryType.CREDIT) {
    restoredBalance = balance.withdraw(entry.getAmount(), txId, now);
} else {
    restoredBalance = balance.deposit(entry.getAmount(), txId, now);
}
```

#### ✅ Best Practice: 도메인에 캡슐화
```java
// JournalEntry.java (도메인 엔티티)
public Balance applyReverseTo(Balance balance, Long transactionId, Instant now) {
    return switch (this.entryType) {
        case CREDIT -> balance.withdraw(amount, transactionId, now);
        case DEBIT -> balance.deposit(amount, transactionId, now);
    };
}

// ReversalService.java (단순화)
Balance restoredBalance = entry.applyReverseTo(balance, txId, now);
```

### 이점
| 관점 | 설명 |
|------|------|
| **확장성** | 새 Enum 값 추가 시 도메인 한 곳만 수정. Switch expression은 누락된 케이스를 **컴파일 에러**로 잡아줌 |
| **테스트 용이성** | 서비스 통합 테스트 없이 도메인 단위 테스트로 핵심 로직 검증 가능 |
| **응집도** | 관련 로직이 데이터(Enum)와 함께 위치 |
| **가독성** | 서비스 코드가 "무엇을 할지"만 표현, "어떻게"는 도메인에 위임 |

### Switch Expression 사용 (Java 14+)
*   Enum 분기에는 `if-else` 대신 **switch expression**을 사용합니다.
*   모든 케이스를 커버하지 않으면 컴파일 에러가 발생하여 확장 시 안전합니다.

---

## 7. Domain Event 생성 원칙 (DDD & OOP)

### 기본 원칙: Tell, Don't Ask

Domain Event는 **도메인 엔티티가 자신의 상태 변화를 표현**하는 책임을 가집니다. 서비스 레이어에서 엔티티의 내부 상태를 꺼내서(Ask) 조립하지 말고, 엔티티에게 이벤트 생성을 명령(Tell)합니다.

### ❌ Anti-Pattern: 서비스에서 이벤트 조립

```java
// DepositService.java
outboxEventRecorder.record(
    LedgerPostedEvent.of(
        savedTransaction.getId(),     // Ask - ID를 꺼냄
        command.accountId(),
        command.amount(),
        TransactionType.DEPOSIT,
        now                           // Ask - 시간을 다시 전달
    )
);
```

**문제점:**
- 서비스가 Transaction의 내부 상태(`id`, `createdAt`)를 직접 접근
- **캡슐화 위반**: `now`를 외부에서 다시 전달 (이미 Transaction이 `createdAt`을 알고 있음)
- 서비스가 "이벤트를 어떻게 만드는지"까지 알아야 함 (SRP 위반)

### ✅ Best Practice: 도메인 엔티티에서 이벤트 생성

```java
// Transaction.java (도메인 엔티티)
public LedgerPostedEvent toPostedEvent(
    Long accountId, 
    BigDecimal amount, 
    TransactionType transactionType) {
    return LedgerPostedEvent.of(
        this.id,          // 내부 상태 사용
        accountId,
        amount,
        transactionType,
        this.createdAt    // 내부 상태 사용
    );
}

public LedgerReversedEvent toReversedEvent(
    Long originalTransactionId, 
    String reason) {
    return LedgerReversedEvent.of(
        this.id,
        originalTransactionId,
        reason,
        this.createdAt
    );
}
```

**서비스 코드 (단순화):**
```java
// DepositService.java
outboxEventRecorder.record(
    savedTransaction.toPostedEvent(
        command.accountId(),
        command.amount(),
        TransactionType.DEPOSIT
    )
);
```

### 장점

| 관점 | 설명 |
|------|------|
| **캡슐화** | Transaction의 내부 상태(`id`, `createdAt`)를 외부에 노출하지 않음 |
| **SRP** | 서비스는 "언제 발행하는지"만, Transaction은 "어떻게 만드는지"만 책임 |
| **Tell, Don't Ask** | 서비스가 엔티티에게 "ID를 달라"고 묻는 대신 "이벤트를 만들어줘"라고 명령 |
| **확장성** | 서비스가 많아져도 이벤트 생성 로직은 Transaction 한 곳에만 존재 |
| **Ubiquitous Language** | `toPostedEvent()` 메서드명이 비즈니스 언어를 그대로 표현 |

### 적용 가이드라인

1. **이벤트는 Aggregate Root에서 생성**
   - Transaction, Order, Account 등 Aggregate Root가 자신의 상태 변화를 이벤트로 표현

2. **외부 컨텍스트는 파라미터로 전달**
   - `accountId`, `amount` 등 이벤트에 필요하지만 엔티티 내부에 없는 정보는 파라미터로 받음
   - 이는 Factory Method 패턴의 일종으로, "내가 만드는 이벤트에 필요한 재료를 달라"는 의미

3. **메서드 네이밍**
   - `to[EventName]()` 패턴 사용 (예: `toPostedEvent()`, `toReversedEvent()`)
   - 비즈니스 언어를 그대로 반영

4. **이벤트 발행은 서비스 트랜잭션 마지막에**
   - 비즈니스 로직 완료 후 `outboxEventRecorder.record()` 호출
   - 주석: `// Publish Domain Event` (명확한 의도 표현)