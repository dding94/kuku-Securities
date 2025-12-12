# Spring @Transactional Deep Dive

> **목적**: 이 문서는 Spring의 `@Transactional` 어노테이션의 내부 동작 원리와 주의사항을 설명합니다.
> 금융 시스템에서 트랜잭션 관리는 데이터 정합성의 핵심이므로, 이 내용을 정확히 이해하는 것이 중요합니다.

---

## 1. @Transactional 프록시 동작 원리

### 1.1 AOP 기반 프록시 메커니즘

Spring의 `@Transactional`은 **AOP(Aspect-Oriented Programming)** 기반으로 동작합니다.

```
[Client] → [Proxy] → [Target Bean]
              ↓
        TransactionInterceptor
              ↓
        PlatformTransactionManager
```

1. **Bean 등록 시점**: Spring 컨테이너가 `@Transactional`이 붙은 클래스를 발견하면 **프록시 객체**를 생성하여 Bean으로 등록합니다.
2. **메서드 호출 시**: 클라이언트가 메서드를 호출하면 프록시가 먼저 호출을 가로챕니다.
3. **트랜잭션 시작**: `TransactionInterceptor`가 `PlatformTransactionManager`를 통해 트랜잭션을 시작합니다.
4. **실제 메서드 실행**: 대상 메서드(비즈니스 로직)를 실행합니다.
5. **커밋/롤백**: 정상 완료 시 커밋, 예외 발생 시 롤백합니다.

### 1.2 프록시 생성 방식

| 방식 | 조건 | 특징 |
|------|------|------|
| **JDK Dynamic Proxy** | 인터페이스가 있는 경우 | 인터페이스 메서드만 가로챔 |
| **CGLIB Proxy** | 인터페이스가 없는 경우 | 클래스를 상속하여 프록시 생성 |

> **Spring Boot 2.0+**: 기본적으로 CGLIB 프록시를 사용합니다.

---

## 2. Self-invocation 문제

### 2.1 문제 상황

같은 클래스 내에서 `@Transactional` 메서드를 호출하면 **트랜잭션이 적용되지 않습니다.**

```java
@Service
public class MyService {
    
    public void outerMethod() {
        // ❌ 프록시를 거치지 않음. 새 트랜잭션 생성 안 됨!
        innerMethod();
    }
    
    @Transactional
    public void innerMethod() {
        // 이 트랜잭션은 동작하지 않음
    }
}
```

**이유**: Self-invocation은 프록시를 거치지 않고 `this.innerMethod()`를 직접 호출하기 때문입니다.

### 2.2 해결 방법

#### ✅ 권장: 클래스 분리

```java
@Service
@RequiredArgsConstructor
public class OuterService {
    private final InnerService innerService;
    
    public void outerMethod() {
        innerService.innerMethod(); // 프록시를 통해 호출
    }
}

@Service
public class InnerService {
    @Transactional
    public void innerMethod() {
        // 트랜잭션 정상 동작
    }
}
```

#### ⚠️ 비권장: AopContext.currentProxy()

```java
@Service
public class MyService {
    public void outerMethod() {
        // 안티패턴 - 가독성 저하, 테스트 어려움
        ((MyService) AopContext.currentProxy()).innerMethod();
    }
}
```

> **실무 권장**: 새로운 트랜잭션이 필요한 로직은 **반드시 별도 클래스로 분리**하세요.

---

## 3. Propagation 옵션

트랜잭션 전파(Propagation)는 이미 트랜잭션이 존재할 때 새 트랜잭션을 어떻게 처리할지 결정합니다.

### 3.1 주요 옵션

| 옵션 | 기존 트랜잭션 있을 때 | 기존 트랜잭션 없을 때 | 사용 사례 |
|------|----------------------|----------------------|----------|
| **REQUIRED** (기본값) | 참여 | 새로 생성 | 대부분의 비즈니스 로직 |
| **REQUIRES_NEW** | 기존 중단, 새로 생성 | 새로 생성 | 독립적인 로깅, 알림 |
| **NESTED** | Savepoint 생성 | 새로 생성 | 부분 롤백이 필요한 경우 |
| **SUPPORTS** | 참여 | 트랜잭션 없이 실행 | 조회 로직 |
| **NOT_SUPPORTED** | 기존 중단, 트랜잭션 없이 실행 | 트랜잭션 없이 실행 | 특정 작업 격리 |
| **MANDATORY** | 참여 | 예외 발생 | 반드시 트랜잭션 필요 |
| **NEVER** | 예외 발생 | 트랜잭션 없이 실행 | 트랜잭션 금지 |

### 3.2 REQUIRES_NEW 주의사항

```java
@Transactional
public void processOrder(Order order) {
    saveOrder(order);
    
    try {
        notificationService.sendNotification(order); // REQUIRES_NEW
    } catch (Exception e) {
        log.warn("Notification failed, but order continues");
    }
}
```

- REQUIRES_NEW는 **새 DB 커넥션**을 사용합니다.
- 커넥션 풀 고갈 주의가 필요합니다.
- 외부 트랜잭션 롤백 시에도 REQUIRES_NEW 트랜잭션은 이미 커밋되었습니다.

### 3.3 NESTED 주의사항

> ⚠️ **MySQL InnoDB에서 NESTED 사용 비권장**

- NESTED는 SAVEPOINT를 사용합니다.
- InnoDB에서 SAVEPOINT는 성능 오버헤드가 있습니다.
- 대안: REQUIRES_NEW를 사용하거나, 로직을 재설계하세요.

---

## 4. Isolation Level 선택

### 4.1 격리 수준 개요

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|-----------|------------|---------------------|--------------|
| READ_UNCOMMITTED | O | O | O |
| READ_COMMITTED | X | O | O |
| **REPEATABLE_READ** (MySQL 기본) | X | X | O (InnoDB: △) |
| SERIALIZABLE | X | X | X |

> **MySQL InnoDB**: Gap Lock을 통해 REPEATABLE_READ에서도 Phantom Read를 상당 부분 방지합니다.

### 4.2 용어 정리

- **Dirty Read**: 커밋되지 않은 다른 트랜잭션의 데이터를 읽음
- **Non-Repeatable Read**: 같은 쿼리를 두 번 실행했는데 결과가 다름 (UPDATE)
- **Phantom Read**: 같은 조건으로 조회했는데 행 수가 달라짐 (INSERT/DELETE)

### 4.3 권장 설정

```java
// 일반적인 조회/갱신
@Transactional(isolation = Isolation.DEFAULT) // MySQL REPEATABLE_READ

// 잔액 조회 후 출금 같은 동시성 민감한 작업
@Transactional // + Optimistic/Pessimistic Lock 별도 적용
```

---

## 5. ReadOnly 최적화

### 5.1 설정 방법

```java
@Transactional(readOnly = true)
public List<Transaction> getTransactionHistory(Long accountId) {
    return transactionPort.findByAccountId(accountId);
}
```

### 5.2 실제 효과

| 최적화 | 설명 | 영향 |
|--------|------|------|
| **Dirty Checking 스킵** | 엔티티 변경 감지 안 함 | 메모리/CPU 절약 |
| **FlushType.MANUAL** | 자동 플러시 비활성화 | DB I/O 감소 |
| **DB 라우팅** | 읽기 전용 복제본 사용 가능 | 마스터 부하 분산 |
| **락 힌트** | 일부 DB에서 공유 락 최적화 | 락 경합 감소 |

### 5.3 주의사항

```java
@Transactional(readOnly = true)
public void readOnlyWithModification() {
    Account account = accountPort.findById(1L);
    account.setName("New Name"); // 변경 감지 안 됨, DB에 반영 안 됨!
}
```

- readOnly=true에서 엔티티 수정은 **자동 반영되지 않습니다**.
- 명시적으로 save()를 호출해야 하며, 이 경우 예외가 발생할 수 있습니다.

---

## 6. 실무 Best Practice

### 6.1 서비스 레이어 규칙

```java
@Service
@RequiredArgsConstructor
public class WithdrawService implements WithdrawUseCase {
    
    @Override
    @Transactional // 명시적으로 선언
    public void withdraw(WithdrawCommand command) {
        // 비즈니스 로직
    }
}
```

- **클래스 레벨** `@Transactional`보다 **메서드 레벨**을 권장합니다.
- 각 메서드의 트랜잭션 요구사항을 명확히 표현할 수 있습니다.

### 6.2 롤백 규칙

```java
// 기본: RuntimeException만 롤백
@Transactional

// Checked Exception도 롤백
@Transactional(rollbackFor = Exception.class)

// 특정 예외는 롤백하지 않음
@Transactional(noRollbackFor = BusinessException.class)
```

### 6.3 트랜잭션 경계 설계

```
[Controller] → [UseCase(Service)] → [Port(Adapter)]
                     ↑
            @Transactional 여기에 적용
```

- 트랜잭션 경계는 **UseCase 레이어**에서 관리합니다.
- Controller나 Adapter에서 트랜잭션을 시작하지 마세요.

---

## 7. 참고 자료

- [Spring Framework - Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [Spring Data JPA - Transactional Queries](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html)
- [MySQL InnoDB Locking](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html)
