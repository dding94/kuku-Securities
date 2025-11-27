# ADR 001: 초기 아키텍처로 MSA(Microservices Architecture) 채택

*   **Status**: Accepted
*   **Date**: 2025-11-27
*   **Author**: dding94

## Context (배경)

"쿠쿠증권"이라는 고성능 트레이딩 플랫폼을 구축하고 있다.
일반적으로 초기 스타트업이나 프로젝트는 생산성을 위해 **Monolithic Architecture**로 시작하는 것이 권장된다.
하지만 본 프로젝트는 단순한 기능 구현이 아닌, **기술적 요구사항(대용량 트래픽, 고가용성, 장애 격리)**을 충족하는 시스템을 설계하고 검증하는 것이 핵심 목표다.

따라서 초기 복잡도를 감수하더라도, 확장성과 장애 격리가 용이한 아키텍처를 선택해야 하는 상황이다.

## Decision (결정)

우리는 프로젝트 초기 단계부터 **MSA(Microservices Architecture)**를 채택하기로 결정했다.
시스템을 다음과 같은 핵심 도메인별로 분리한다:

1.  **Core Ledger (원장)**: 데이터 무결성이 최우선인 도메인.
2.  **Order System (주문)**: 고동시성 처리와 빠른 응답이 필요한 도메인.
3.  **Market Data (시세)**: 대량의 실시간 데이터를 처리해야 하는 도메인.
4.  **API Gateway**: 단일 진입점 및 공통 관심사(인증/인가) 처리.

## Rationale (근거)

1.  **장애 격리 (Fault Isolation)**:
    *   시세 서비스에 트래픽이 폭주하여 장애가 발생하더라도, 원장(자산) 서비스나 주문 접수 서비스는 정상 동작해야 한다.
    *   Monolithic 구조에서는 하나의 모듈(예: 시세)의 메모리 누수가 전체 시스템의 셧다운을 유발할 수 있다.

2.  **기술 스택의 유연성 (Polyglot & Optimization)**:
    *   **원장/주문**: 데이터 정합성이 중요하므로 RDBMS와 트랜잭션 관리에 강한 Spring Boot + JPA (Blocking I/O)가 적합하다.
    *   **시세**: 수많은 클라이언트에게 실시간 데이터를 푸시해야 하므로 Netty 기반의 Spring WebFlux (Non-blocking I/O)가 적합하다.
        *   **Why WebFlux?**: 시세 데이터는 초당 수천 건 이상의 업데이트가 발생하며, 수만 명의 클라이언트가 동시에 연결(WebSocket)을 유지해야 한다.
        *   기존의 Thread-per-Request 모델(Tomcat/MVC)은 동시 접속자 수가 늘어날수록 스레드 컨텍스트 스위칭 비용이 급증하여 메모리와 CPU 효율이 떨어진다.
        *   반면, WebFlux(Netty)는 소수의 이벤트 루프 스레드로 대량의 동시 연결을 효율적으로 처리할 수 있어, 실시간 스트리밍 서비스에 필수적이다.
        *   **Alternatives Considered (대안 비교)**:
            *   **Spring MVC + Servlet 3.1 Async**: 비동기 처리는 가능하지만, 여전히 Tomcat의 스레드 풀 모델에 의존하므로 C10K 문제 해결에는 한계가 있다.
            *   **Vert.x**: 성능은 WebFlux보다 우수할 수 있으나, Spring 생태계(Security, Actuator 등)와의 통합 비용이 크고 러닝 커브가 높다. 팀의 주 기술 스택인 Spring과의 정합성을 위해 WebFlux를 선택했다.
    *   MSA를 통해 각 서비스에 최적화된 기술 스택을 적용할 수 있다.

3.  **조직적 확장성 (Organizational Scalability)**:
    *  각 도메인 팀이 독립적으로 배포하고 운영할 수 있는 구조를 지향한다.

## Trade-offs (트레이드오프)

### 장점 (Pros)
*   서비스별 독립적인 배포 및 스케일링(Scale-out) 가능.
*   특정 서비스의 장애가 전체로 전파되는 것을 방지 (Circuit Breaker 도입 전제).
*   도메인별 최적의 기술 스택 선택 가능.

### 단점 (Cons)
*   **초기 개발 복잡도 증가**: 분산 시스템 환경 구축(Service Discovery, Gateway, Tracing 등)에 비용이 든다.
*   **데이터 일관성 유지의 어려움**: 분산 트랜잭션 문제(Eventual Consistency 필요)가 발생한다.
*   **운영 복잡도 증가**: 모니터링, 로깅, 디버깅이 단일 애플리케이션보다 훨씬 어렵다.

## Mitigation (단점 완화 방안)
*   **복잡도 관리**: Docker Compose와 Kubernetes를 활용하여 로컬 개발 환경을 자동화한다.
*   **데이터 일관성**: 이벤트 기반 아키텍처(Event-Driven Architecture)를 도입하여 결과적 일관성(Eventual Consistency)을 확보한다.
*   **관측성 확보**: ELK Stack을 도입하여 분산 환경에서의 가시성을 확보한다.
