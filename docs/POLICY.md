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

### Testing
*   **TDD (Test-Driven Development)**: 가능한 경우 테스트를 먼저 작성하고 구현합니다.
*   **Coverage**: 도메인 로직에 대해서는 높은 테스트 커버리지를 유지합니다.
*   **Test Pyramid**: Unit Test > Integration Test > E2E Test 비율을 유지합니다.
*   **Deterministic Testing**: 테스트는 언제 실행해도 동일한 결과를 보장해야 합니다.
    *   `LocalDateTime.now()`, `Random` 등을 직접 사용하지 말고, **고정된 값(Fixed Value)**을 주입하거나 Mocking하여 테스트하세요.

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

### Event-Driven Architecture
서비스 간의 결합도를 낮추기 위해 비동기 메시징(Kafka)을 적극 활용합니다.
*   **Eventual Consistency**: 분산 트랜잭션 대신 이벤트를 통한 결과적 일관성을 추구합니다.
