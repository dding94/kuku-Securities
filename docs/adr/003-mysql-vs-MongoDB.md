# ADR 003: MySQL vs MongoDB for Ledger System

## Status
Accepted

## Context
금융 원장(Ledger) 시스템의 핵심 데이터 저장소(RDBMS/NoSQL)를 선정해야 한다.
원장 시스템은 다음과 같은 **극한의 요구사항**을 가진다.

1.  **Write-Heavy & Append-Only**: 거래는 끊임없이 발생하며, 절대 수정되거나 삭제되지 않는(Immutable) 이력 데이터다.
2.  **Sequential Access (Range Scan)**: "특정 계좌의 최근 1개월 거래 내역"과 같이 시간순(TSID) 범위 조회가 빈번하다.
3.  **Data Integrity (ACID)**: 돈과 관련된 데이터이므로 트랜잭션의 원자성(Atomicity)과 영속성(Durability)이 생명이다.
4.  **Operational Stability**: 장애 발생 시 복구 절차가 명확하고, 숙련된 엔지니어링 리소스가 풍부해야 한다.

## Decision
**MySQL (InnoDB Engine)** 을 선택한다.

## Detailed Analysis

### 1. MySQL (InnoDB) - The Selected One
*   **Architecture**: **Clustered Index** 구조.
*   **Pros**:
    *   **Clustered Index & Sequential I/O**: PK(TSID) 기준으로 데이터가 물리적으로 정렬되어 저장된다. 따라서 시간순 범위 조회(Range Scan) 시 디스크 헤더의 이동을 최소화하는 Sequential I/O가 보장된다.
    *   **Direct Data Access**: PK가 곧 데이터 페이지의 주소이므로, 별도의 테이블 룩업(Heap Fetch) 과정 없이 즉시 데이터에 접근한다.
    *   **Undo Log & MVCC**: 롤백 세그먼트(Undo Log)를 통해 MVCC를 구현하므로, 읽기 작업이 쓰기 작업을 차단하지 않으며(Non-locking Read), 별도의 Vacuum 프로세스가 필요 없다.
*   **Cons**:
    *   Secondary Index Lookup 시 PK를 거쳐야 하므로(Double Lookup), 보조 인덱스가 많을수록 성능이 저하될 수 있다. (하지만 원장은 주로 PK/Time 기반 조회다.)

### 2. MongoDB - The NoSQL Option
*   **Architecture**: Document Store (BSON).
*   **Why Not?**:
    *   **Financial Conservatism & Strict Typing**: 금융 시스템은 보수적인 기술 선택이 미덕이다. RDBMS의 강력하고 엄격한 데이터 타입 보장(Strict Typing)은 돈을 다루는 시스템에서 필수적인 안전장치다. MongoDB의 유연함은 오히려 독이 될 수 있다.
    *   **Schema Rigidity**: 금융 데이터는 엄격한 스키마 관리가 필수다. Schemaless는 초기 개발 속도에는 좋으나, 데이터 정합성이 생명인 원장 시스템에서는 오히려 **데이터 오염의 위험**이 된다.
    *   **Join Complexity**: 원장 데이터와 계좌/사용자 정보를 조인하여 리포팅해야 하는 경우, `$lookup` 등의 연산 비용이 비싸다.

## Consequences
*   **MySQL 8.0+ (InnoDB)** 를 확정 사용한다.
*   **PK 설계**: TSID를 사용하여 Clustered Index의 장점(Insert 성능 + Range Scan 성능)을 극대화한다.
*   **인덱스 전략**: 불필요한 Secondary Index를 최소화하여 Write 성능을 보존한다.
